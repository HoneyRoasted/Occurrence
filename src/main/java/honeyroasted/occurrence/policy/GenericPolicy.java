package honeyroasted.occurrence.policy;

import honeyroasted.occurrence.generics.JavaType;

public interface GenericPolicy<T> {

    Class<T> target();

    JavaType generics(T event);

}
