package honeyroasted.occurrence;

public class InvokeMethodException extends RuntimeException {

    public InvokeMethodException() {
    }

    public InvokeMethodException(String message) {
        super(message);
    }

    public InvokeMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvokeMethodException(Throwable cause) {
        super(cause);
    }

    public InvokeMethodException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
