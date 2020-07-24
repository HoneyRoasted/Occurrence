package honeyroasted.occurrence.generator.visitors;

import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.ConstructorParams;
import honeyroasted.occurrence.generator.FilterVisitor;
import honeyroasted.occurrence.generator.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.generics.ReflectionUtil;
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

        Optional<Class> source = annotation.get("source", Class.class);
        Class<?> targetClass = this.listener ? listenerMethod.getDeclaringClass() : source.orElse(input.getEffectiveType());

        int size = annotation.numIndexed() + (listener || source.isPresent() ? 1 : 0);

        for (Method m : targetClass.getMethods()) {
            if (m.getName().equals(method)) {
                if ((source.isEmpty() || Modifier.isStatic(m.getModifiers())) && (!listener || Modifier.isStatic(m.getModifiers()) || !Modifier.isStatic(listenerMethod.getModifiers()))) {
                    if (size == m.getParameterCount()) {
                        boolean found = true;

                        for (int i = 0; i < size; i++) {
                            Class<?> parameter = m.getParameterTypes()[i];
                            if (listener || source.isPresent()) {
                                if (i == 0) {
                                    if (!parameter.isAssignableFrom(input.getEffectiveType())) {
                                        found = false;
                                    }
                                } else if (annotation.get(String.valueOf(i - 1), parameter).isEmpty()) {
                                    found = false;
                                }
                            } else {
                                if (annotation.get(String.valueOf(i), parameter).isEmpty()) {
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
            invoke = invokeVirtual(src, method, signature, input.getEffectiveType().isInterface());
        }

        if (listener || source.isPresent()) {
            invoke.arg(get(current));
        }

        for (int i = 0; i < size; i++) {
            if (listener || source.isPresent()) {
                if (i > 0) {
                    String name = nameProvider.provide(method + "_arg");
                    Class<?> param = target.getParameterTypes()[i];
                    constructorParams.add(name, annotation.require(String.valueOf(i - 1), param), type(param));
                    invoke.arg(constructorParams.get(name));
                }
            } else {
                String name = nameProvider.provide(method + "_arg");
                Class<?> param = target.getParameterTypes()[i];
                constructorParams.add(name, annotation.require(String.valueOf(i), param), type(param));
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
            return new Result(res, JavaType.of(target.getGenericReturnType()));
        }
    }

    @Override
    public boolean filterOnly() {
        return this.predicate;
    }


}
