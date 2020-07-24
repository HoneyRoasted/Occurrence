package honeyroasted.occurrence.manager;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;

public interface EventManager<T> extends EventDispatch<T>, ListenerRegistry<T>, Closeable {

    static <T> EventManager<T> createNew() {
        return synchronize(newBakingManager());
    }

    static <T> EventManager<T> newThreadedManager() {
        return threaded(createNew());
    }

    static <T> EventManager<T> newSimpleManager() {
        return new SimpleEventManager<>();
    }

    static <T> EventManager<T> newBakingManager() {
        return new BakingEventManager<>();
    }

    static <T> EventManager<T> threaded(EventManager<T> manager) {
        return new ThreadedEventManager<>(manager);
    }

    static <T> EventManager<T> synchronize(EventManager<T> manager) {
        return new SynchronizedEventManager<>(manager);
    }

    static <T extends Comparable<T>> List<T> newSortedList() {
        return Collections.synchronizedList(new SortedList<T>());
    }

    @Override
    default void close() {

    }
}
