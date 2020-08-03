package honeyroasted.occurrence.generator.bytecode;

import honeyroasted.occurrence.InvalidFilterException;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.InvokeMethodException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.annotation.FilterWrapperBuilder;
import honeyroasted.occurrence.generator.ListenerWrapperGenerator;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class BytecodeListenerWrapperGenerator<T> implements ListenerWrapperGenerator<T> {
    private static long uniqueSuffix = 0;

    private ClassLoader loader;
    private VisitorRegistry registry;

    private Map<Class<?>, Collection<Function<Object, ListenerWrapper<?>>>> instanceListeners = new WeakHashMap<>();
    private Map<Class<?>, Collection<ListenerWrapper<?>>> staticListeners = new WeakHashMap<>();

    public BytecodeListenerWrapperGenerator(ClassLoader loader, VisitorRegistry registry) {
        this.loader = loader;
        this.registry = registry;
    }

    @Override
    public Collection<ListenerWrapper<T>> generate(Object obj, PolicyRegistry registry) {
        Collection<ListenerWrapper<T>> wrappers = new ArrayList<>();
        if (obj != null) {
            Class<?> cls = obj.getClass();
            instanceListeners.computeIfAbsent(cls, key -> {
                Collection<Result> results = gen(obj, this.registry, registry);
                Collection<Function<Object, ListenerWrapper<?>>> funcs = new ArrayList<>();

                for (Result r : results) {
                    ByteArrayClassLoader byteArrayClassLoader = new ByteArrayClassLoader(this.loader);
                    byte[] bytes = r.getClassNode().toByteArray();
                    Class<?> loaded = byteArrayClassLoader.defineClass(r.getClassNode().getSignature().writeInternalName().replace('/', '.'), bytes);
                    Object[] params = r.params.genParams();
                    params[0] = null;

                    funcs.add(o -> {
                        try {
                            Object[] cstmParams = Arrays.copyOf(params, params.length);
                            cstmParams[0] = o;
                            return (ListenerWrapper<?>) loaded.getDeclaredConstructors()[0].newInstance(cstmParams);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new InvokeMethodException("Failed to invoke generated class constructor, this is likely an internal error", e);
                        }
                    });
                }

                return funcs;
            }).forEach(f -> wrappers.add((ListenerWrapper<T>) f.apply(obj)));
        }

        return wrappers;
    }

    @Override
    public Collection<ListenerWrapper<T>> generate(Class<?> cls, PolicyRegistry registry) {
        return (Collection<ListenerWrapper<T>>) (Collection) this.staticListeners.computeIfAbsent(cls, key ->
                genWrappers(gen(key, this.registry, registry), this.loader));
    }

    public static Collection<ListenerWrapper<?>> genWrappers(Collection<Result> results, ClassLoader loader) {
        return results.stream().sorted().map(r -> {
            ByteArrayClassLoader byteArrayClassLoader = new ByteArrayClassLoader(loader);

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
            if (FilterWrapperBuilder.of(ReflectionUtil.getAnnotations(method)).stream().anyMatch(f -> f.getId().equals("listener")) && !Modifier.isStatic(method.getModifiers())) {
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
            if (FilterWrapperBuilder.of(ReflectionUtil.getAnnotations(method)).stream().anyMatch(f -> f.getId().equals("listener")) && Modifier.isStatic(method.getModifiers())) {
                res.add(gen(method, listener, listener, visitorRegistry, policyRegistry, true));
            }
        }

        return res;
    }

    public static Result gen(Method method, Object listener, Class<?> listenerClass, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry, boolean staticListener) {
        List<FilterWrapper> wrappers = FilterWrapperBuilder.of(ReflectionUtil.getAnnotations(method));
        if (wrappers.stream().noneMatch(f -> f.getId().equals("listener"))) {
            throw new InvalidListenerException("Listener method must be annotated with listener filter", method);
        } else {
            FilterWrapper annotation = wrappers.stream().filter(f -> f.getId().equals("listener")).findFirst().get();

            JavaType event = null;
            List<List<Annotation>> annotations = ReflectionUtil.getParameterAnnotations(method);
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                try {
                    List<FilterWrapper> filters = FilterWrapperBuilder.of(annotations.get(i));
                    if (filters.stream().allMatch(f -> visitorRegistry.get(f.getId()).map(FilterVisitor::filterOnly).orElse(false)) && event == null) {
                        event = JavaType.of(parameter.getParameterizedType());
                    }
                } catch (InvalidFilterException e) {
                    throw new InvalidListenerException("Invalid filter on param: " + parameter.getName(), method, e);
                }
            }

            if (event == null) {
                event = JavaType.of(ReflectionUtil.getCommonParent(annotation.get("event", Class[].class).orElse(new Class[]{Object.class})));
            }

            ClassNode classNode = classDef(ACC_PUBLIC, classSignature(parameterized("Lhoneyroasted/occurrence/generated/" + listenerClass.getSimpleName().replace('/', '_') + "$" + method.getName().replace('/', '_') + "$" + System.identityHashCode(listener) + "$" + (uniqueSuffix++) + ";"))
                    .addInterface(type(ListenerWrapper.class).addPart(event.toPecansType())));

            return new Result(method.getName(), classNode, gen(annotation, classNode, method, listener, listenerClass, event, visitorRegistry, policyRegistry, staticListener));
        }
    }

    public static ConstructorParams gen(FilterWrapper annotation, ClassNode node, Method method, Object listener, Class<?> listenerClass, JavaType event, VisitorRegistry visitorRegistry, PolicyRegistry policyRegistry, boolean staticListener) {
        MethodNode init = method(ACC_PUBLIC, "<init>", methodSignature(VOID));
        Sequence initSeq = sequence();
        init.body(initSeq);
        node.add(init);

        String name = BytecodeListenerWrapperGenerator.createHandleName(listenerClass, method, listener);
        ConstructorParams params = new ConstructorParams();
        params.add("listener", listener);

        TypeInformal eventType = event.getEffectiveType().isPrimitive() ? type(ReflectionUtil.box(event.getEffectiveType())) : event.toPecansType();

        genNonHandleMethods(node, params, annotation, name, event.getEffectiveType());

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
        List<List<Annotation>> parameterAnnotations = ReflectionUtil.getParameterAnnotations(method);
        NameProvider nameProvider = new NameProvider();

        String evVar = "event";
        JavaType evGenType = event;

        for (FilterWrapper wrapper : FilterWrapperBuilder.of(ReflectionUtil.getAnnotations(method))) {
            Optional<FilterVisitor> visitorOptional = visitorRegistry.get(wrapper.getId());

            if (visitorOptional.isPresent()) {
                FilterVisitor visitor = visitorOptional.get();
                if (!visitor.filterOnly()) {
                    throw new InvalidListenerException("Listener method annotation must be filter only", method);
                }

                FilterVisitor.Result result = visitor.visitTransform(sequence, wrapper, evVar, evGenType, params, policyRegistry, nameProvider, method);
                evVar = result.getVariable();
                evGenType = result.getReturnType();
            }
        }

        for (int i = 0; i < parameterAnnotations.size(); i++) {
            List<Annotation> annotations = parameterAnnotations.get(i);
            List<FilterWrapper> filters = FilterWrapperBuilder.of(annotations);

            String var = evVar;
            JavaType type = evGenType;

            for (FilterWrapper wrapper : filters) {
                Optional<FilterVisitor> visitorOptional = visitorRegistry.get(wrapper.getId());

                if (visitorOptional.isPresent()) {
                    FilterVisitor visitor = visitorOptional.get();
                    FilterVisitor.Result result = visitor.visitTransform(sequence, wrapper, var, type, params, policyRegistry, nameProvider, method);
                    var = result.getVariable();
                    type = result.getReturnType();
                }
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
            invoke = invokeVirtual(params.get("listener"), method.getName(), sig);
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
                        getType = invokeVirtual(params.get(srcName), invocable.getMethod().getName(),
                                methodSignature(invocable.getMethod()))
                                .arg(get(var));

                    } else {
                        getType = invokeVirtual(get(var), invocable.getMethod().getName(), methodSignature(invocable.getMethod()));
                    }
                } else {
                    String policyName = nameProvider.provide("generic_policy");
                    params.add(policyName, policy);
                    getType = invokeInterface(params.get(policyName), "generics",
                            methodSignature(type(JavaType.class), type(Object.class)))
                            .arg(get(var));
                }

                String targetName = nameProvider.provide("param_type");
                params.add(targetName, JavaType.of(method.getGenericParameterTypes()[finalI]));
                sequence.add(ifBlock(not(invokeVirtual(params.get(targetName), "isAssignableFrom", methodSignature(BOOLEAN, type(JavaType.class)))
                        .arg(invokeVirtual(
                                getType,
                                "getSuperType", methodSignature(type(JavaType.class), type(Class.class)))
                                .arg(invokeVirtual(params.get(targetName), "getEffectiveType", methodSignature(type(Class.class)))))), ret()));
            });
        }

        if (condition != null) {
            sequence.add(ifBlock(condition, invoke))
                    .add(ret());
        } else {
            sequence.add(invoke)
                    .add(ret());
        }

        initSeq.add(invokeSpecial(type(Object.class), loadThis(), "<init>", methodSignature(VOID)));
        params.getParamTypes().forEach((param, type) -> {
            node.add(field(ACC_PRIVATE, param, type));
            init.param(param, type);
            initSeq.add(set(loadThis(), param, get(param)));
        });
        initSeq.add(ret());

        return params;
    }

    public static void genNonHandleMethods(ClassNode node, ConstructorParams params, FilterWrapper listener, String name, Class<?> event) {
        params.add("priority", listener.require("priority", int.class), INT);
        params.add("name", name);

        node.add(method(ACC_PUBLIC, "priority", methodSignature(INT),
                ret(params.get("priority"))));

        node.add(method(ACC_PUBLIC, "name", methodSignature(type(String.class)),
                ret(params.get("name"))));

        node.add(method(ACC_PUBLIC, "listener", methodSignature(type(Object.class)),
                ret(params.get("listener"))));

        node.add(method(ACC_PUBLIC, "event", methodSignature(type(Class.class).addPart(wildcard())),
                ret(constant(ReflectionUtil.box(event)))));
    }

    public static String createHandleName(Class<?> cls, Method method, Object owner) {
        return cls.getName().replace('/', '_') + "#" + method.getName().replace('/', '_') + "(" + String.join(", ", Stream.of(method.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new)) + ")$" + System.identityHashCode(owner);
    }

    public static class Result implements Comparable<Result> {
        private String method;
        private ClassNode classNode;
        private ConstructorParams params;

        public Result(String method, ClassNode classNode, ConstructorParams params) {
            this.classNode = classNode;
            this.params = params;
            this.method = method;
        }

        public ClassNode getClassNode() {
            return classNode;
        }

        public ConstructorParams getParams() {
            return params;
        }

        @Override
        public int compareTo(Result o) {
            return this.method.compareTo(o.method);
        }
    }

}
