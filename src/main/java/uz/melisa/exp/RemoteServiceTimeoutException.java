package uz.melisa.exp;

public class RemoteServiceTimeoutException extends RuntimeException {
    public RemoteServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteServiceTimeoutException(String message) {
        super(message);
    }
}
