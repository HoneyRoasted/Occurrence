package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.javatype.ArrayType;
import honeyroasted.javatype.GenericType;
import honeyroasted.javatype.JavaType;
import honeyroasted.javatype.JavaTypes;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.manager.ReflectionUtil;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.TypedNode;
import honeyroasted.pecans.node.instruction.invocation.Invoke;
import honeyroasted.pecans.type.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class InvokeFilter implements FilterVisitor {
    private boolean listener;
    private boolean predicate;
    private String id;

    public InvokeFilter(boolean listener, boolean predicate, String id) {
        this.listener = listener;
        this.predicate = predicate;
        this.id = id;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        String method = annotation.require("value", String.class);
        Method target = null;

        Optional<Class> source = annotation.get("source", Class.class).map(c -> c.equals(Void.class) ? null : c);
        Class<?> targetClass = this.listener ? listenerMethod.getDeclaringClass() : source.orElse(input.getType());

        int size = annotation.arraySize() + (listener || source.isPresent() ? 1 : 0);

        for (Method m : targetClass.getMethods()) {
            if (m.getName().equals(method)) {
                if ((!source.isPresent() || Modifier.isStatic(m.getModifiers())) && (!listener || Modifier.isStatic(m.getModifiers()) || !Modifier.isStatic(listenerMethod.getModifiers()))) {
                    if (size == m.getParameterCount()) {
                        boolean found = true;

                        for (int i = 0; i < size; i++) {
                            Class<?> parameter = m.getParameterTypes()[i];
                            if (listener || source.isPresent()) {
                                if (i == 0) {
                                    if (!parameter.isAssignableFrom(input.getType())) {
                                        found = false;
                                    }
                                } else if (!annotation.get(i - 1, parameter).isPresent()) {
                                    found = false;
                                }
                            } else {
                                if (!annotation.get(i, parameter).isPresent()) {
                                    found = false;
                                }
                            }
                        }

                        if (found) {
                            target = m;
                            break;
                        }
                    }
                }
            }
        }

        if (target == null) {
            throw new InvalidListenerException("No target method: " + method + " found on: " + targetClass.getName() + " for given arguments", listenerMethod);
        } else if (this.predicate && !target.getReturnType().equals(boolean.class) && !target.getReturnType().equals(Boolean.class)) {
            throw new InvalidListenerException("Expected return type: boolean on method: " + ReflectionUtil.nameAndSig(target) + " got: " + target.getReturnType().getName(), listenerMethod);
        }

        MethodSignature signature = methodSignature(type(target.getReturnType()));
        for (Class<?> parameter : target.getParameterTypes()) {
            signature.addParameter(type(parameter));
        }

        Invoke invoke;

        if (Modifier.isStatic(target.getModifiers())) {
            invoke = invokeStatic(type(targetClass), method, signature);
        } else {
            TypedNode src;
            if (listener) {
                src = constructorParams.get("listener");
            } else {
                src = get(current);
            }

            if (input.getType().isInterface()) {
                invoke = invokeInterface(src, method, signature);
            } else {
                invoke = invokeVirtual(src, method, signature, false);
            }
        }

        if (listener || source.isPresent()) {
            Class<?> type = target.getParameterTypes()[0];
            invoke.arg(convert(type(type), get(current)));
        }

        for (int i = 0; i < size; i++) {
            if (listener || source.isPresent()) {
                if (i > 0) {
                    String name = nameProvider.provide(method + "_arg");
                    Class<?> param = target.getParameterTypes()[i];
                    constructorParams.add(name, annotation.require(i - 1, param), type(param));
                    invoke.arg(constructorParams.get(name));
                }
            } else {
                String name = nameProvider.provide(method + "_arg");
                Class<?> param = target.getParameterTypes()[i];
                constructorParams.add(name, annotation.require(i, param), type(param));
                invoke.arg(constructorParams.get(name));
            }
        }

        if (predicate) {
            TypedNode value = invoke;
            if (target.getReturnType().equals(Boolean.class)) {
                value = unbox(value);
            }

            node.add(ifBlock(not(value), ret()));

            return new Result(current, input);
        } else {
            String res = nameProvider.provide();
            node.add(def(res, invoke));

            JavaType ret = JavaTypes.of(target.getGenericReturnType());
            if (input instanceof GenericType) {
                ret = ret.resolveVariables((GenericType) input, JavaTypes.ofParameterized(input.getType()));
            } else if (input instanceof ArrayType && ((ArrayType) input).getAbsoluteComponent() instanceof GenericType) {
                ret = ret.resolveVariables((GenericType) ((ArrayType) input).getAbsoluteComponent(), JavaTypes.ofParameterized(((ArrayType) input).getAbsoluteComponent().getType()));
            }

            return new Result(res, ret);
        }
    }

    @Override
    public boolean filterOnly() {
        return this.predicate;
    }


}
