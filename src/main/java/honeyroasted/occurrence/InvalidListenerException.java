package honeyroasted.occurrence;

import honeyroasted.occurrence.manager.ReflectionUtil;

import java.lang.reflect.Method;

public class InvalidListenerException extends RuntimeException {

    public InvalidListenerException(String message, Method method) {
        super("[Invalid method: " + ReflectionUtil.nameAndSig(method) + "]: " + message);
    }

    public InvalidListenerException(String message, Method method, Throwable e) {
        super("[Invalid method: " + ReflectionUtil.nameAndSig(method) + "]: " + message, e);
    }

}
