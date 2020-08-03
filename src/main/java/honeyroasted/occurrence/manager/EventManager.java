package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.generator.bytecode.BytecodeListenerWrapperGenerator;
import honeyroasted.occurrence.generator.bytecode.VisitorRegistry;
import honeyroasted.occurrence.policy.PolicyRegistry;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;

public interface EventManager<T> extends EventDispatch<T>, ListenerRegistry<T>, Closeable {

    static <T> EventManager<T> createNew(Class<?> source) {
        return synchronize(new BakingEventManager<>(PolicyRegistry.GLOBAL,
                new BytecodeListenerWrapperGenerator<>(source.getClassLoader(), VisitorRegistry.GLOBAL), source.getClassLoader()));
    }

    static <T> EventManager<T> newSimpleManage(Class<?> source) {
        return new SimpleEventManager<>(PolicyRegistry.GLOBAL,
                new BytecodeListenerWrapperGenerator<>(source.getClassLoader(), VisitorRegistry.GLOBAL));
    }

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
