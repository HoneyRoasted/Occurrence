package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.ListenerWrapper;

import java.util.function.Predicate;

public interface ListenerRegistry<T> {

    void register(ListenerWrapper<T> listener);

    void unregister(Predicate<ListenerWrapper<T>> remove);

    void register(Object listener);

    void register(Class<?> listener);

    default void unregister(Object listener) {
        this.unregister(l -> l.listener() == listener);
    }

    default void unregister(Class<?> listener) {
        this.unregister(l -> l.listener() == listener);
    }
}
