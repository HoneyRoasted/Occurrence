package honeyroasted.occurrence.policy;

import honeyroasted.javatype.JavaType;
import honeyroasted.occurrence.InvokeMethodException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InvocableGenericPolicy<T> implements GenericPolicy<T> {
    private Object source;
    private Method method;

    private Class<T> target;

    public InvocableGenericPolicy(Object source, Method method, Class<T> target) {
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

    public Class<T> getTarget() {
        return target;
    }

    @Override
    public Class<T> target() {
        return this.target;
    }

    @Override
    public JavaType generics(T event) {
        try {
            if (this.source != null) {
                return (JavaType) this.method.invoke(this.source, event);
            } else {
                return (JavaType) this.method.invoke(event);
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new InvokeMethodException("Failed to invoke method: " + method.getName(), ex);
        }
    }

}
