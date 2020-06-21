package honeyroasted.occurrence;

public class IllegalFilterException extends RuntimeException {

    public IllegalFilterException() {
    }

    public IllegalFilterException(String message) {
        super(message);
    }

    public IllegalFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalFilterException(Throwable cause) {
        super(cause);
    }

    public IllegalFilterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
