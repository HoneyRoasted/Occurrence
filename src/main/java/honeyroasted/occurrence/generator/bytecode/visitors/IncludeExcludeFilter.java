package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.generics.ReflectionUtil;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.TypedNode;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class IncludeExcludeFilter implements FilterVisitor {
    private boolean include;
    private String id;

    public IncludeExcludeFilter(boolean include, String id) {
        this.include = include;
        this.id = id;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        List<Class> classes = Stream.of(annotation.require("value", Class[].class)).collect(Collectors.toList());
        JavaType result;

        if (include) {
            result = ReflectionUtil.getReverseRespect(classes, input).orElse(input);
        } else {
            result = input;
        }

        TypedNode condition = null;

        for (Class cls : classes) {
            if (!cls.equals(input.getEffectiveType())) {
                if ((cls.isPrimitive() && !input.isPrimitive()) ||
                    (!cls.isPrimitive() && input.isPrimitive()) ||
                        ((!JavaType.of(cls).isNumericPrimitive() || !input.isNumericPrimitive()) && cls.isPrimitive() && input.isPrimitive())) {
                    throw new InvalidListenerException(input.getEffectiveType().getName() + " cannot be assigned to " + cls.getName(), listenerMethod);
                } else if (!cls.isPrimitive() && !input.isPrimitive()) {
                    TypedNode inst = instanceOf(get(current), type(cls));
                    if (condition == null) {
                        condition = inst;
                    } else {
                        condition = or(condition, inst);
                    }
                }
            }
        }

        if (condition != null) {
            if (include) {
                condition = not(condition);
            }

            node.add(ifBlock(condition, ret()));
        }

        return new Result(current, include ? result : input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }


}
