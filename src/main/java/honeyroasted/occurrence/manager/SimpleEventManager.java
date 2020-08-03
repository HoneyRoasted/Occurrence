package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.generator.ListenerWrapperGenerator;
import honeyroasted.occurrence.generator.bytecode.BytecodeListenerWrapperGenerator;
import honeyroasted.occurrence.generator.bytecode.VisitorRegistry;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.util.ByteArrayClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SimpleEventManager<T> implements EventManager<T> {
    private List<ListenerWrapper<T>> wrappers = EventManager.newSortedList();

    private PolicyRegistry policyRegistry;
    private ListenerWrapperGenerator<T> gen;

    public SimpleEventManager() {
        this(PolicyRegistry.GLOBAL, new BytecodeListenerWrapperGenerator<>(ClassLoader.getSystemClassLoader(), VisitorRegistry.GLOBAL));
    }

    public SimpleEventManager(PolicyRegistry policyRegistry, ListenerWrapperGenerator<T> generator) {
        this.policyRegistry = policyRegistry;
        this.gen = generator;
    }

    @Override
    public void post(T event) throws HandleEventException {
        List<HandleEventException.Entry> errors = null;

        for (ListenerWrapper<T> wrapper : this.wrappers) {
            if (wrapper.event().isInstance(event)) {
                try {
                    wrapper.handle(event);
                } catch (Throwable throwable) {
                    if (errors == null) {
                        errors = new ArrayList<>();
                    }

                    errors.add(new HandleEventException.Entry(throwable, wrapper));
                }
            }
        }

        if (errors != null) {
            throw new HandleEventException(event, errors);
        }
    }

    @Override
    public void register(ListenerWrapper<T> wrapper) {
        this.wrappers.add(wrapper);
    }

    @Override
    public void unregister(Predicate<ListenerWrapper<T>> remove) {
        this.wrappers.removeIf(remove);
    }

    @Override
    public void register(Object listener) {
        this.wrappers.addAll(this.gen.generate(listener, this.policyRegistry));
    }

    @Override
    public void register(Class<?> listener) {
        this.wrappers.addAll(this.gen.generate(listener, this.policyRegistry));
    }

    @Override
    public Collection<ListenerWrapper<T>> listeners() {
        return Collections.unmodifiableCollection(new ArrayList<>(this.wrappers));
    }

}
