package honeyroasted.occurrence.generator.bytecode.visitors;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class IterableAllFilter implements FilterVisitor {

    @Override
    public String id() {
        return "iterable.all";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        List<Class> classes = Stream.of(annotation.require("value", Class[].class)).collect(Collectors.toList());
        Optional<? extends JavaType> iterType = input.resolveToSupertype(Iterable.class);
        JavaType result = iterType.<JavaType>map(type -> type.isGeneric() ? ((GenericType) type).getGeneric(0)
                .resolveToSupertype(JavaTypes.getCommonParent(classes)).orElse(null) : null).orElse(JavaTypes.of(Object.class));
        result = GenericType.builder(List.class).generics(result.isArray() ? ((GenericType) result).getGenerics() : Arrays.asList(JavaTypes.OBJECT)).build();

        String name = nameProvider.provide("iterable_all");

        if (input.isPrimitive()) {
            throw new InvalidListenerException(input.getType().getName() + " cannot be assigned to " + Iterable.class.getName(), listenerMethod);
        }

        node.add(ifBlock(not(instanceOf(get(current), type(Iterable.class))), ret()));

        String iterator = nameProvider.provide("iterator");
        node.add(def(iterator, invokeInterface(convert(type(Iterable.class), get(current)), "iterator", methodSignature(type(Iterator.class)))));

        TypedNode condition = null;

        String it = nameProvider.provide("it");

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

        node.add(def(name, newObj(type(ArrayList.class), methodSignature(VOID)).type(type(List.class))));
        node.add(whileLoop(
                invokeInterface(get(iterator), "hasNext", methodSignature(BOOLEAN)),
                sequence(
                        def(it, invokeInterface(get(iterator), "next", methodSignature(OBJECT))),
                        condition == null ?
                                invokeInterface(get(name), "add", methodSignature(BOOLEAN, OBJECT))
                                        .arg(get(it)).pop() :
                                ifBlock(condition, invokeInterface(get(name), "add", methodSignature(BOOLEAN, OBJECT))
                                                    .arg(get(it)).pop()))
        ));

        return new Result(name, result);
    }

}
