package honeyroasted.occurrence.policy;

import honeyroasted.occurrence.InvokeMethodException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InvocableCancellablePolicy<T> implements CancellablePolicy<T> {
    private Object source;
    private Method method;

    private Class<T> target;

    public InvocableCancellablePolicy(Object source, Method method, Class<T> target) {
        this.source = source;
        this.method = method;
        this.target = target;
    }

    public Object getSource() {
        return source;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public Class<T> target() {
        return this.target;
    }

    @Override
    public boolean isCancelled(T event) {
        try {
            if (this.source != null) {
                return (boolean) this.method.invoke(this.source, event);
            } else {
                return (boolean) this.method.invoke(event);
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new InvokeMethodException("Failed to invoke method: " + method.getName(), ex);
        }
    }
}
