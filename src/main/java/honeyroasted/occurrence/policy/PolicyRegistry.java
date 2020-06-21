package honeyroasted.occurrence.policy;

import honeyroasted.occurrence.event.CancellableEvent;
import honeyroasted.occurrence.event.GenericEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PolicyRegistry {
    public static final PolicyRegistry GLOBAL = new PolicyRegistry();

    static {
        GLOBAL.registerDefaults();
    }

    private List<GenericPolicy<?>> genericPolicies = new ArrayList<>();
    private List<CancellablePolicy<?>> cancellablePolicies = new ArrayList<>();


    public void registerDefaults() {
        register(CancellableEvent.policy());
        register(GenericEvent.policy());
    }

    public void register(GenericPolicy<?> policy) {
        this.genericPolicies.add(policy);
    }

    public void register(CancellablePolicy<?> policy) {
        this.cancellablePolicies.add(policy);
    }

    public Optional<GenericPolicy<?>> genericPolicy(Object type) {
        return this.genericPolicies.stream().filter(g -> g.target().isInstance(type)).findFirst();
    }

    public Optional<CancellablePolicy<?>> cancellablePolicy(Object type) {
        return this.cancellablePolicies.stream().filter(g -> g.target().isInstance(type)).findFirst();
    }

}
