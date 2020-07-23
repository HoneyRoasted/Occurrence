package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;
import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.generator.ListenerWrapperGenerator;
import honeyroasted.occurrence.generator.VisitorRegistry;
import honeyroasted.occurrence.policy.PolicyRegistry;
import honeyroasted.pecans.util.ByteArrayClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class SimpleEventManager<T> implements EventManager<T> {
    private List<ListenerWrapper<T>> wrappers = new SortedList<>();

    private PolicyRegistry policyRegistry;
    private VisitorRegistry visitorRegistry;
    private ClassLoader loader;

    public SimpleEventManager() {
        this(PolicyRegistry.GLOBAL, VisitorRegistry.GLOBAL, ClassLoader.getSystemClassLoader());
    }

    public SimpleEventManager(PolicyRegistry policyRegistry, VisitorRegistry visitorRegistry, ClassLoader loader) {
        this.policyRegistry = policyRegistry;
        this.visitorRegistry = visitorRegistry;
        this.loader = loader;
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
        this.wrappers.addAll((Collection) ListenerWrapperGenerator.genWrappers(
                ListenerWrapperGenerator.gen(listener, visitorRegistry, policyRegistry), new ByteArrayClassLoader(this.loader)));
    }

    @Override
    public void register(Class<?> listener) {
        this.wrappers.addAll((Collection) ListenerWrapperGenerator.genWrappers(
                ListenerWrapperGenerator.gen(listener, visitorRegistry, policyRegistry), new ByteArrayClassLoader(this.loader)));
    }

}
