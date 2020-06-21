package honeyroasted.occurrence.event;

import honeyroasted.occurrence.policy.CancellablePolicy;
import honeyroasted.occurrence.policy.InvocableCancellablePolicy;

public interface CancellableEvent {

    boolean isCancelled();

    void setCancelled(boolean value);

    static CancellablePolicy<CancellableEvent> policy() {
        try {
            return new InvocableCancellablePolicy<>(null, CancellableEvent.class.getMethod("isCancelled"), CancellableEvent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
