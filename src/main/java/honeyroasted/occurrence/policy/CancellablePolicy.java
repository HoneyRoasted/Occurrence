package honeyroasted.occurrence.policy;

public interface CancellablePolicy<T> {

    Class<T> target();

    boolean isCancelled(T event);

}
