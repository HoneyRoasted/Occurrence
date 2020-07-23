package honeyroasted.occurrence.generator;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.InvokeMethodException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.annotation.Listener;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.generics.ReflectionUtil;
import honeyroasted.occurrence.policy.InvocableGenericPolicy;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.ClassNode;
import honeyroasted.pecans.node.MethodNode;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.TypedNode;
import honeyroasted.pecans.node.instruction.invocation.Invoke;
import honeyroasted.pecans.type.MethodSignature;
import honeyroasted.pecans.type.Types;
import honeyroasted.pecans.type.type.TypeInformal;
import honeyroasted.pecans.util.ByteArrayClassLoader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class ListenerWrapperGenerator {
    private static long uniqueSuffix = 0;

    public static Collection<ListenerWrapper<?>> genWrappers(Collection<Result> results, ClassLoader loader) {
        return results.stream().map(r -> {
            ByteArrayClassLoader byteArrayClassLoader;
            if (loader instanceof ByteArrayClassLoader) {
                byteArrayClassLoader = (ByteArrayClassLoader) loader;
            } else {
                byteArrayClassLoader = new ByteArrayClassLoader(loader);
            }

            byte[] cls = r.getClassNode().toByteArray();
            Class<?> loaded = byteArrayClassLoader.defineClass(r.getClassNode().getSignature().writeInternalName().replace('/', '.'), cls);
            try {
                return (ListenerWrapper<?>) loaded.getDeclaredConstructors()[0].newInstance(r.getParams().genParams());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new InvokeMethodException("Failed to invoke generated class constructor, this is likely an internal error", e);
            }
        }).collect(Collectors.toList());
    }

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
            throw new InvalidListenerException("Listener method must be annotated with @Listener", method);
        } else {
            JavaType event = null;
            for (Parameter parameter : method.getParameters()) {
                try {
                    List<FilterWrapper> filters = FilterWrapper.of(parameter.getAnnotations());
                    if (filters.stream().allMatch(f -> visitorRegistry.get(f.getId()).map(FilterVisitor::filterOnly).orElse(false)) && event == null) {
                        event = JavaType.of(parameter.getParameterizedType());
                    }
                } catch (InvalidFilterException e) {
                    throw new InvalidListenerException("Invalid filter on param: " + parameter.getName(), method, e);
                }
            }

            if (event == null) {
                event = JavaType.of(Object.class);
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

        TypeInformal eventType = event.getEffectiveType().isPrimitive() ? type(ReflectionUtil.box(event.getEffectiveType())) : event.toPecansType();

        if (!eventType.equals(OBJECT) && !eventType.writeDesc().equals(OBJECT.writeDesc())) {
            node.add(method(ACC_PUBLIC | ACC_BRIDGE, "handle", methodSignature(VOID))
                    .param("event", OBJECT)
                    .body(
                            sequence(
                                    invokeVirtual(loadThis(), "handle", methodSignature(VOID, eventType).addException(type(Throwable.class)), false)
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
        JavaType[] varTypes = new JavaType[vars.length];
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        NameProvider nameProvider = new NameProvider();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            List<FilterWrapper> filters = FilterWrapper.of(annotations);

            String var = "event";
            JavaType type = event;
            for (FilterWrapper wrapper : filters) {
                FilterVisitor visitor = visitorRegistry.get(wrapper.getId()).orElseThrow(() -> new InvalidListenerException("No filter registered for: " + wrapper.getId(), method));

                FilterVisitor.Result result = visitor.visitTransform(sequence, wrapper, var, type, params, policyRegistry, nameProvider, method);
                var = result.getVariable();
                type = result.getReturnType();
            }
            vars[i] = var;
            varTypes[i] = type;
        }

        MethodSignature sig = methodSignature(JavaType.of(method.getGenericReturnType()).toPecansType());
        for (Type type : method.getGenericParameterTypes()) {
            sig.addParameter(JavaType.of(type).toPecansType());
        }

        Invoke invoke;
        TypedNode condition = null;

        if (staticListener) {
            invoke = invokeStatic(type(listenerClass), method.getName(), sig);
        } else {
            invoke = invokeVirtual(get(loadThis(), "listener", type(listenerClass)), method.getName(), sig);
        }

        Map<String, Class<?>> seenTests = new HashMap<>();

        for (int i = 0; i < vars.length; i++) {
            String var = vars[i];
            JavaType paramType = JavaType.of(method.getParameterTypes()[i]);
            JavaType varType = varTypes[i];

            if (!paramType.getEffectiveType().isAssignableFrom(paramType.getEffectiveType()) || paramType.getEffectiveType().isPrimitive() || varType.getEffectiveType().isPrimitive()) {
                if ((varType.getEffectiveType().isPrimitive() || paramType.getEffectiveType().isPrimitive()) &&
                        !(varType.getEffectiveType().isPrimitive() && paramType.getEffectiveType().isAssignableFrom(ReflectionUtil.box(varType.getEffectiveType()))) &&
                        !(varType.isNumericPrimitive() && paramType.isNumericPrimitive()) &&
                        !ReflectionUtil.unbox(varType.getEffectiveType()).equals(ReflectionUtil.unbox(paramType.getEffectiveType()))) {
                    throw new InvalidListenerException(varType + " is not assignable to " + paramType, method);
                } else if (!paramType.getEffectiveType().equals(seenTests.get(var)) && !paramType.getEffectiveType().isPrimitive() && !varType.getEffectiveType().isPrimitive()) {
                    seenTests.put(var, paramType.getEffectiveType());
                    TypedNode ins = instanceOf(get(var), type(ReflectionUtil.box(paramType.getEffectiveType())));
                    if (condition == null) {
                        condition = ins;
                    } else {
                        condition = and(condition, ins);
                    }
                }

                invoke.arg(convert(paramType.toPecansType(), get(var)));
            } else {
                invoke.arg(get(var));
            }

            int finalI = i;
            policyRegistry.genericPolicy(varType.getEffectiveType()).ifPresent(policy -> {
                TypedNode getType;
                if (policy instanceof InvocableGenericPolicy<?>) {
                    InvocableGenericPolicy<?> invocable = (InvocableGenericPolicy<?>) policy;
                    if (invocable.getSource() != null) {
                        String srcName = nameProvider.provide("generic_source");
                        params.add(srcName, invocable.getSource());
                        getType = invokeVirtual(get(loadThis(), srcName, params.getType(srcName)), invocable.getMethod().getName(),
                                methodSignature(invocable.getMethod()))
                                .arg(get(var));

                    } else {
                        getType = invokeVirtual(get(var), invocable.getMethod().getName(), methodSignature(invocable.getMethod()));
                    }
                } else {
                    String policyName = nameProvider.provide("generic_policy");
                    params.add(policyName, policy);
                    getType = invokeInterface(get(loadThis(), policyName, params.getType(policyName)), "generics",
                            methodSignature(type(JavaType.class), type(Object.class)))
                        .arg(get(var));
                }

                String targetName = nameProvider.provide("param_type");
                params.add(targetName, JavaType.of(method.getGenericParameterTypes()[finalI]));
                sequence.add(ifBlock(not(invokeVirtual(get(loadThis(), targetName, params.getType(targetName)), "isAssignableFrom", methodSignature(BOOLEAN, type(JavaType.class)))
                        .arg(invokeVirtual(
                                getType,
                                "getSuperType", methodSignature(type(JavaType.class), type(Class.class)))
                                .arg(invokeVirtual(get(loadThis(), targetName, params.getType(targetName)), "getEffectiveType", methodSignature(type(Class.class)))))), ret()));
            });
        }

        if (condition != null) {
            sequence.add(ifBlock(condition, invoke))
                    .add(ret());
        } else {
            sequence.add(invoke)
                    .add(ret());
        }

        genNonHandleMethods(node, params, annotation, name, eventType, event.getEffectiveType(), listener);

        initSeq.add(invokeSpecial(type(Object.class), loadThis(), "<init>", methodSignature(VOID)));
        params.getParamTypes().forEach((param, type) -> {
            node.add(field(ACC_PRIVATE, param, type));
            init.param(param, type);
            initSeq.add(set(loadThis(), param, get(param)));
        });
        initSeq.add(ret());

        return params;
    }

    public static void genNonHandleMethods(ClassNode node, ConstructorParams params, Listener listener, String name, TypeInformal eventType, Class<?> event, Object listenerObj) {
        String prioVar = params.add("priority", listener.priority(), INT);
        String nameVar = params.add("name", name);
        String lisVar = params.add("listener", listenerObj);

        node.add(method(ACC_PUBLIC, "priority", methodSignature(INT),
                ret(get(loadThis(), prioVar, INT))));

        node.add(method(ACC_PUBLIC, "name", methodSignature(type(String.class)),
                ret(get(loadThis(), nameVar, type(String.class)))));

        node.add(method(ACC_PUBLIC, "event", methodSignature(type(Class.class).addPart(wildcard())),
                ret(constant(ReflectionUtil.box(event)))));

        node.add(method(ACC_PUBLIC, "listener", methodSignature(type(Object.class)),
                ret(get(loadThis(), lisVar, type(listenerObj.getClass())))));

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
