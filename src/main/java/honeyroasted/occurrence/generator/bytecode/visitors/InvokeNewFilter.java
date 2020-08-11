package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.javatype.JavaType;
import honeyroasted.javatype.JavaTypes;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.invocation.Invoke;
import honeyroasted.pecans.type.MethodSignature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class InvokeNewFilter implements FilterVisitor {

    @Override
    public String id() {
        return "invoke.new";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        Class<?> src = annotation.require("value", Class.class);
        int size = annotation.arraySize() + 1;

        Constructor target = null;

        for (Constructor c : src.getConstructors()) {
            if (c.getParameterCount() == size) {
                boolean found = true;

                for (int i = 0; i < size; i++) {
                    Class<?> parameter = c.getParameterTypes()[i];
                    if (i == 0) {
                        if (!parameter.isAssignableFrom(input.getType())) {
                            found = false;
                        }
                    } else if (!annotation.get(i - 1, parameter).isPresent()) {
                        found = false;
                    }
                }

                if (found) {
                    target = c;
                    break;
                }
            }
        }

        if (target == null) {
            throw new InvalidListenerException("No constructor found on: " + src.getName() + " for given arguments", listenerMethod);
        }

        MethodSignature signature = methodSignature(VOID);
        for (Class<?> parameter : target.getParameterTypes()) {
            signature.addParameter(type(parameter));
        }

        String name = nameProvider.provide();

        Invoke invoke = newObj(type(src), signature);
        invoke.arg(get(current));

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                String argName = nameProvider.provide("new_arg");
                Class<?> param = target.getParameterTypes()[i - 1];
                constructorParams.add(argName, annotation.require(i - 1, param));
                invoke.arg(constructorParams.get(argName));
            }
        }

        node.add(def(name, invoke));

        return new Result(name, JavaTypes.of(src));
    }

}
