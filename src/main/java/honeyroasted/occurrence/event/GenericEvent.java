package honeyroasted.occurrence.event;

import honeyroasted.javatype.JavaType;
import honeyroasted.occurrence.policy.GenericPolicy;
import honeyroasted.occurrence.policy.InvocableGenericPolicy;

public interface GenericEvent {

    JavaType type();

    static GenericPolicy<GenericEvent> policy() {
        try {
            return new InvocableGenericPolicy<>(null, GenericEvent.class.getMethod("type"), GenericEvent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
