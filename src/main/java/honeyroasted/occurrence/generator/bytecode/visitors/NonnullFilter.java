package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;

import java.lang.reflect.Method;

import static honeyroasted.pecans.node.Nodes.*;

public class NonnullFilter implements FilterVisitor {

    @Override
    public String id() {
        return "nonnull";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        if (!input.isPrimitive()) {
            node.add(ifBlock(equalsNull(get(current)), ret()));
        }
        return new Result(current, input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }

}
