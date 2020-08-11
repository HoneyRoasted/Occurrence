package honeyroasted.occurrence.policy;

import honeyroasted.javatype.JavaType;

public interface GenericPolicy<T> {

    Class<T> target();

    JavaType generics(T event);

}
