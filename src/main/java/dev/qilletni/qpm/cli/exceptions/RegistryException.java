package dev.qilletni.qpm.cli.exceptions;

/**
 * Exception thrown when registry operations fail (network errors, API errors, etc.).
 */
public class RegistryException extends Exception {
    private final int statusCode;
    private final String errorCode;

    public RegistryException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorCode = null;
    }

    public RegistryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = null;
    }

    public RegistryException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorCode = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
