package honeyroasted.occurrence.generator.visitors;

import honeyroasted.occurrence.InvalidListenerException;
import honeyroasted.occurrence.annotation.FilterWrapper;
import honeyroasted.occurrence.generator.ConstructorParams;
import honeyroasted.occurrence.generator.FilterVisitor;
import honeyroasted.occurrence.generator.NameProvider;
import honeyroasted.occurrence.generics.JavaType;
import honeyroasted.occurrence.policy.CancellablePolicy;
import honeyroasted.occurrence.policy.InvocableCancellablePolicy;
import honeyroasted.occurrence.policy.InvocableGenericPolicy;
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
        boolean cancelled = annotation.get("value", Boolean.class).orElse(false);

        Optional<CancellablePolicy<?>> policyOptional = policyRegistry.cancellablePolicy(input.getEffectiveType());
        if (policyOptional.isPresent()) {
            CancellablePolicy<?> policy = policyOptional.get();
            TypedNode getCancelled;
            if (policy instanceof InvocableCancellablePolicy) {
                InvocableCancellablePolicy<?> invocable = (InvocableCancellablePolicy<?>) policy;
                if (invocable.getSource() != null) {
                    String srcName = nameProvider.provide("cancellable_source");
                    constructorParams.add(srcName, invocable.getSource());

                    getCancelled = invokeVirtual(constructorParams.get(srcName), invocable.getMethod().getName(), methodSignature(invocable.getMethod()))
                        .arg(get(current));
                } else {
                    getCancelled = invokeVirtual(get(current), invocable.getMethod().getName(), methodSignature(invocable.getMethod()));
                }
            } else {
                String policyName = nameProvider.provide("cancellable_policy");
                constructorParams.add(policyName, policy);
                getCancelled = invokeInterface(constructorParams.get(policyName), "isCancelled", methodSignature(BOOLEAN, type(Object.class)))
                    .arg(get(current));
            }

            if (cancelled) {
                node.add(ifBlock(not(getCancelled), ret()));
            } else {
                node.add(ifBlock(getCancelled, ret()));
            }
        } else {
            throw new InvalidListenerException("No cancellable policy for: " + input.getEffectiveType().getName(), listenerMethod);
        }

        return new Result(current, input);
    }

    @Override
    public boolean filterOnly() {
        return true;
    }


}
