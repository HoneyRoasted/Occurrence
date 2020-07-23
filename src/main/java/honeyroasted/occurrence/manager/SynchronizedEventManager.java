package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;
import honeyroasted.occurrence.ListenerWrapper;

import java.util.function.Predicate;

public class SynchronizedEventManager<T> implements EventManager<T> {
    private EventDispatch<T> dispatch;
    private ListenerRegistry<T> registry;

    public SynchronizedEventManager(EventDispatch<T> dispatch, ListenerRegistry<T> registry) {
        this.dispatch = dispatch;
        this.registry = registry;
    }

    public SynchronizedEventManager(EventManager<T> backing) {
        this(backing, backing);
    }

    @Override
    public void post(T event) throws HandleEventException {
        synchronized (this) {
            this.dispatch.post(event);
        }
    }

    @Override
    public void register(ListenerWrapper<T> wrapper) {
        synchronized (this) {
            this.registry.register(wrapper);
        }
    }

    @Override
    public void unregister(Predicate<ListenerWrapper<T>> remove) {
        synchronized (this) {
            this.registry.unregister(remove);
        }
    }

    @Override
    public void register(Object listener) {
        synchronized (this) {
            this.registry.register(listener);
        }
    }

    @Override
    public void register(Class<?> listener) {
        synchronized (this) {
            this.registry.register(listener);
        }
    }
}
