package honeyroasted.occurrence.manager;

import honeyroasted.occurrence.HandleEventException;

public interface EventDispatch<T> {

    void post(T event) throws HandleEventException;

}
