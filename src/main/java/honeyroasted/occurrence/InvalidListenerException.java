package honeyroasted.occurrence;

import java.lang.reflect.Method;
import java.util.stream.Stream;

public class InvalidListenerException extends RuntimeException {

    public InvalidListenerException(String message, Method method, Class listenerClass) {
        super("[Invalid method: " + listenerClass.getName() + "#" + method.getName() + "(" + Stream.of(method.getParameters()).map(p -> p.getType().getSimpleName()).reduce((a, b) -> a + ", " + b).orElse("") + ")]: " + message);
    }

    public InvalidListenerException(String message, Method method, Class listenerClass, Throwable e) {
        super("[Invalid method: " + listenerClass.getName() + "#" + method.getName() + "(" + Stream.of(method.getParameters()).map(p -> p.getType().getSimpleName()).reduce((a, b) -> a + ", " + b).orElse("") + ")]: " + message, e);
    }

}
