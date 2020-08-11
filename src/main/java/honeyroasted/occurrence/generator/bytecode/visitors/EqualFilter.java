package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.javatype.JavaType;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.TypedNode;

import java.lang.reflect.Method;
import java.util.Objects;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class EqualFilter implements FilterVisitor {

    @Override
    public String id() {
        return "equal";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        Object val = annotation.require("value", input.getType());
        String name = nameProvider.provide("equal_test");
        constructorParams.add(name, val);

        TypedNode cond;
        if (input.isPrimitive()) {
            cond = equal(get(current), constructorParams.get(name));
        } else {
            cond = invokeStatic(type(Objects.class), "equals", methodSignature(BOOLEAN, OBJECT, OBJECT))
                    .arg(input.isPrimitive() ? convert(OBJECT, get(current)) : get(current))
                    .arg(constructorParams.getType(name).isPrimitive() ? convert(OBJECT, constructorParams.get(name)) : constructorParams.get(name));
        }

        node.add(ifBlock(not(cond), ret()));

        return new Result(current, input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }

}
