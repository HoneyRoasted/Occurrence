package honeyroasted.occurrence.generator.bytecode.visitors;

import honeyroasted.javatype.JavaType;
import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.annotation.Tristate;
import honeyroasted.occurrence.generator.bytecode.ConstructorParams;
import honeyroasted.occurrence.generator.bytecode.FilterVisitor;
import honeyroasted.occurrence.generator.bytecode.NameProvider;
import honeyroasted.occurrence.policy.CancellablePolicy;
import honeyroasted.occurrence.policy.InvocableCancellablePolicy;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.node.instruction.Sequence;
import honeyroasted.pecans.node.instruction.TypedNode;

import java.lang.reflect.Method;
import java.util.Optional;

import static honeyroasted.pecans.node.Nodes.*;
import static honeyroasted.pecans.type.Types.*;

public class CancelledFilter implements FilterVisitor {

    @Override
    public String id() {
        return "cancelled";
    }

    @Override
    public Result visitTransform(Sequence node, FilterWrapper annotation, String current, JavaType input, ConstructorParams constructorParams, PolicyRegistry policyRegistry, NameProvider nameProvider, Method listenerMethod) {
        Tristate cancelled = annotation.require("value", Tristate.class);

        Optional<CancellablePolicy<?>> policyOptional = policyRegistry.cancellablePolicy(input.getType());
        if (policyOptional.isPresent()) {
            CancellablePolicy<?> policy = policyOptional.get();
            TypedNode getCancelled;
            if (policy instanceof InvocableCancellablePolicy) {
                InvocableCancellablePolicy<?> invocable = (InvocableCancellablePolicy<?>) policy;
                if (invocable.getSource() != null) {
                    String srcName = nameProvider.provide("cancellable_source");
                    constructorParams.add(srcName, invocable.getSource());

                    getCancelled = invokeVirtual(constructorParams.get(srcName), invocable.getMethod().getName(), methodSignature(invocable.getMethod()), invocable.getSource().getClass().isInterface())
                        .arg(get(current));
                } else {
                    getCancelled = invokeVirtual(get(current), invocable.getMethod().getName(), methodSignature(invocable.getMethod()), input.getType().isInterface());
                }
            } else {
                String policyName = nameProvider.provide("cancellable_policy");
                constructorParams.add(policyName, policy);
                getCancelled = invokeInterface(constructorParams.get(policyName), "isCancelled", methodSignature(BOOLEAN, type(Object.class)))
                    .arg(get(current));
            }

            if (cancelled == Tristate.TRUE) {
                node.add(ifBlock(not(getCancelled), ret()));
            } else if (cancelled == Tristate.FALSE) {
                node.add(ifBlock(getCancelled, ret()));
            }
        } else if (cancelled == Tristate.TRUE) {
            throw new InvalidListenerException("No cancellable policy for: " + input.getType().getName(), listenerMethod);
        }

        return new Result(current, input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }


}
