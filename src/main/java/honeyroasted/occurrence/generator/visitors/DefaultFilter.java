package honeyroasted.occurrence.generator.visitors;

import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.ConstructorParams;
import honeyroasted.occurrence.generator.FilterVisitor;
import honeyroasted.occurrence.generator.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;

import java.lang.reflect.Method;


public class DefaultFilter implements FilterVisitor {

    @Override
    public String id() {
        return "default";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        return new Result(current, input);
    }

}
