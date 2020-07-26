package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;
import honeyroasted.occurrence.ListenerWrapper;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ThreadedEventManager<T> implements EventManager<T> {
    private EventManager<T> manager;
    private ExecutorService service;

    private Consumer<HandleEventException> catcher;

    public ThreadedEventManager(EventManager<T> manager) {
        this(manager, Throwable::printStackTrace);
    }

    public ThreadedEventManager(EventManager<T> manager, Consumer<HandleEventException> catcher) {
        this(manager, catcher, 1);
    }

    public ThreadedEventManager(EventManager<T> manager, Consumer<HandleEventException> catcher, int threads) {
        this.manager = manager;
        this.catcher = catcher;
        this.service = Executors.newFixedThreadPool(threads);
    }

    @Override
    public void post(T event) throws HandleEventException {
        this.service.submit(() -> {
            try {
                manager.post(event);
            } catch (HandleEventException e) {
                this.catcher.accept(e);
            }
        });
    }

    @Override
    public void register(ListenerWrapper<T> listener) {
        this.service.submit(() -> manager.register(listener));
    }

    @Override
    public void unregister(Predicate<ListenerWrapper<T>> remove) {
        this.service.submit(() -> manager.unregister(remove));
    }

    @Override
    public void register(Object listener) {
        this.service.submit(() -> manager.register(listener));
    }

    @Override
    public void register(Class<?> listener) {
        this.service.submit(() -> manager.register(listener));
    }

    @Override
    public Collection<ListenerWrapper<T>> listeners() {
        return manager.listeners();
    }

    @Override
    public void close() {
        this.service.shutdown();
    }

    public ExecutorService getService() {
        return service;
    }

}
