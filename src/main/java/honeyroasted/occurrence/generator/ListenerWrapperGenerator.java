package honeyroasted.occurrence.generator;

import honeyroasted.occurrence.ListenerWrapper;
import honeyroasted.occurrence.policy.PolicyRegistry;

import java.util.Collection;

public interface ListenerWrapperGenerator<T> {

    Collection<ListenerWrapper<T>> generate(Object obj, PolicyRegistry registry);

    Collection<ListenerWrapper<T>> generate(Class<?> cls, PolicyRegistry registry);

}
