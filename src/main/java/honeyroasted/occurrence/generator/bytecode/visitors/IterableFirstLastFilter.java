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
import honeyroasted.pecans.node.instruction.util.NoopNode;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class IterableFirstLastFilter implements FilterVisitor {
    private boolean first;
    private String id;

    public IterableFirstLastFilter(boolean first, String id) {
        this.first = first;
        this.id = id;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        List<Class> classes = Stream.of(annotation.require("value", Class[].class)).collect(Collectors.toList());

        Optional<JavaType> iterType = ReflectionUtil.getWithRespect(input, Iterable.class);
        JavaType result = iterType.map(type -> ReflectionUtil.getWithRespect(type.getGenerics().getActualParameter(0),
                ReflectionUtil.getCommonParent(classes)).orElse(JavaType.of(Object.class))).orElse(JavaType.of(Object.class));

        String name = nameProvider.provide("iterable_" + (first ? "first" : "last"));

        if (input.isPrimitive()) {
            throw new InvalidListenerException(input.getEffectiveType().getName() + " cannot be assigned to " + Iterable.class.getName(), listenerMethod);
        }

        node.add(ifBlock(not(instanceOf(get(current), type(Iterable.class))), ret()));

        String iterator = nameProvider.provide("iterator");
        node.add(def(iterator, invokeInterface(convert(type(Iterable.class), get(current)), "iterator", methodSignature(type(Iterator.class)))));

        String it = nameProvider.provide("it");

        TypedNode condition = null;
        for (Class cls : classes) {
            if (cls.isPrimitive()) {
                throw new InvalidListenerException("Cannot instanceof on primitive type: " + cls.getName(), listenerMethod);
            }

            if (condition == null) {
                condition = instanceOf(get(it), type(cls));
            } else {
                condition = or(condition, instanceOf(get(it), type(cls)));
            }
        }

        node.add(def(name, constant(null).type(result.toPecansType())));
        node.add(whileLoop(
                invokeInterface(get(iterator), "hasNext", methodSignature(BOOLEAN)),
                sequence(
                        def(it, invokeInterface(get(iterator), "next", methodSignature(OBJECT))),
                        condition == null ?
                                sequence(set(name, get(it)), breakLoop()) :
                                ifBlock(condition, sequence(set(name, get(it)),
                                        this.first ? breakLoop() : new NoopNode()))
                )
        ));

        node.add(ifBlock(equalsNull(get(name)), ret()));

        return new Result(name, result);
    }

}
