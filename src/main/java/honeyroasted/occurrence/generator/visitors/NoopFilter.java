package honeyroasted.occurrence.generator.visitors;

import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.ConstructorParams;
import honeyroasted.occurrence.generator.FilterVisitor;
import honeyroasted.occurrence.generator.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;

import java.lang.reflect.Method;

public class NoopFilter implements FilterVisitor {
    private String id;

    public NoopFilter(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        return new Result(current, input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }

}
