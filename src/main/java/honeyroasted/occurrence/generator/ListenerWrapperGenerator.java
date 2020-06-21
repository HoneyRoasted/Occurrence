package honeyroasted.occurrence.generator;

import honeyroasted.occurrence.IllegalFilterException;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.ClassNode;
import honeyroasted.pecans.node.MethodNode;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.invocation.Invoke;
import honeyroasted.pecans.node.instruction.invocation.InvokeStatic;
import honeyroasted.pecans.type.MethodSignature;
import honeyroasted.pecans.type.type.TypeInformal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class ListenerWrapperGenerator {
    private static long uniqueSuffix = 0;

    public static Collection<Result> gen(Object listener, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }

        List<Result> res = new ArrayList<>();

        for (Method method : listener.getClass().getMethods()) {
            if (method.isAnnotationPresent(Listener.class) && !Modifier.isStatic(method.getModifiers())) {
                res.add(gen(method, listener, listener.getClass(), visitorRegistry, policyRegistry, false));
            }
        }

        return res;
    }

    public static Collection<Result> gen(Class<?> listener, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }

        List<Result> res = new ArrayList<>();

        for (Method method : listener.getMethods()) {
            if (method.isAnnotationPresent(Listener.class) && Modifier.isStatic(method.getModifiers())) {
                res.add(gen(method, listener, listener, visitorRegistry, policyRegistry, true));
            }
        }

        return res;
    }

    public static Result gen(Method method, Object listener, Class<?> listenerClass, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry, boolean staticListener) {
        if (!method.isAnnotationPresent(Listener.class)) {
            throw new InvalidListenerException("Listener method must be annotated with @Listener", method, listenerClass);
        } else {
            JavaType event = JavaType.of(Object.class);
            for (Parameter parameter : method.getParameters()) {
                try {
                    List<FilterWrapper> filters = FilterWrapper.of(parameter.getAnnotations());
                    if (filters.isEmpty()) {
                        event = JavaType.of(parameter.getParameterizedType());
                    }
                } catch (IllegalFilterException e) {
                    throw new InvalidListenerException("Invalid filter on param: " + parameter.getName(), method, listenerClass, e);
                }
            }

            ClassNode classNode = classDef(ACC_PUBLIC, classSignature(parameterized("Lhoneyroasted/occurrence/generated/" + listenerClass.getSimpleName() + "$" + method.getName() + "$" + System.identityHashCode(listener) + "$" + (uniqueSuffix++) + ";"))
                    .addInterface(type(ListenerWrapper.class).addPart(event.toPecansType())));

            return new Result(classNode, gen(classNode, method, listener, listenerClass, event, visitorRegistry, policyRegistry, staticListener));
        }
    }

    public static ConstructorParams gen(ClassNode node, Method method, Object listener, Class<?> listenerClass, JavaType event, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry, boolean staticListener) {
        MethodNode init = method(ACC_PUBLIC, "<init>", methodSignature(VOID));
        Sequence initSeq = sequence();
        init.body(initSeq);
        node.add(init);

        Listener annotation = method.getAnnotation(Listener.class);
        String name = ListenerWrapperGenerator.createHandleName(listenerClass, method, listener);
        ConstructorParams params = new ConstructorParams();
        params.add("listener", listener);

        TypeInformal eventType = event.toPecansType();

        if (!event.equals(Object.class)) {
            node.add(method(ACC_PUBLIC | ACC_BRIDGE, "handle", methodSignature(VOID))
                    .param("event", OBJECT)
                    .body(
                            sequence(
                                    invokeVirtual(loadThis(), "handle", methodSignature(VOID).addException(type(Throwable.class)), false)
                                            .arg(checkcast(eventType, get("event"))),
                                    ret()
                            )
                    ));
        }

        MethodNode impl = method(ACC_PUBLIC, "handle", methodSignature(VOID).addException(type(Throwable.class)))
                .param("event", eventType);
        node.add(impl);
        Sequence sequence = sequence();
        impl.body(sequence);

        String[] vars = new String[method.getParameters().length];
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            List<FilterWrapper> filters = FilterWrapper.of(annotations);

            String var = "event";
            JavaType type = event;
            for (FilterWrapper wrapper : filters) {
                FilterVisitor visitor = visitorRegistry.get(wrapper.getId()).orElseThrow(() -> new InvalidListenerException("", method, listenerClass));

                visitor.visitCondition(sequence, wrapper, var, type, params, policyRegistry);

                FilterVisitor.Result result = visitor.visitTransform(sequence, wrapper, var, type, params, policyRegistry);
                var = result.getVariable();
                type = result.getReturnType();
            }
            vars[i] = var;
        }

        MethodSignature sig = methodSignature(JavaType.of(method.getGenericReturnType()).toPecansType());
        for (Type type : method.getGenericParameterTypes()) {
            sig.addParameter(JavaType.of(type).toPecansType());
        }

        if (staticListener) {
            InvokeStatic invoke = invokeStatic(type(listenerClass), method.getName(), sig);
            for (String var : vars) {
                invoke.arg(get(var));
            }
            sequence.add(invoke)
                    .add(ret());
        } else {
            Invoke invoke = invokeVirtual(get(loadThis(), "listener", type(listenerClass)), method.getName(), sig);
            for (String var : vars) {
                invoke.arg(get(var));
            }
            sequence.add(invoke)
                    .add(ret());
        }


        genNonHandleMethods(node, params, annotation, name);

        initSeq.add(invokeSpecial(type(Object.class), loadThis(), "<init>", methodSignature(VOID)));
        params.getParamTypes().forEach((param, type) -> {
            node.add(field(ACC_PRIVATE, param, type));
            init.param(param, type);
            initSeq.add(set(loadThis(), param, get(param)));
        });
        initSeq.add(ret());

        return params;
    }

    public static void genNonHandleMethods(ClassNode node, ConstructorParams params, Listener listener, String name) {
        String prioVar = params.add("priority", listener.priority(), INT);
        String nameVar = params.add("name", name);

        node.add(method(ACC_PUBLIC, "priority", methodSignature(INT),
                ret(get(loadThis(), prioVar, INT))));

        node.add(method(ACC_PUBLIC, "name", methodSignature(type(String.class)),
                ret(get(loadThis(), nameVar, type(String.class)))));

    }

    public static String createHandleName(Class<?> cls, Method method, Object owner) {
        return cls.getName() + "#" + method.getName() + "(" + String.join(", ", Stream.of(method.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new)) + ")$" + System.identityHashCode(owner);
    }

    public static class Result {
        private ClassNode classNode;
        private ConstructorParams params;

        public Result(ClassNode classNode, ConstructorParams params) {
            this.classNode = classNode;
            this.params = params;
        }

        public ClassNode getClassNode() {
            return classNode;
        }

        public ConstructorParams getParams() {
            return params;
        }
    }

}
