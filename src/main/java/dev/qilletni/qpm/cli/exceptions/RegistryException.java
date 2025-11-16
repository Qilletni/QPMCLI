package dev.qilletni.qpm.cli.exceptions;

/**
 * Exception thrown when registry operations fail (network errors, API errors, etc.).
 */
public class RegistryException extends Exception {
    private final int statusCode;

    public RegistryException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public RegistryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
