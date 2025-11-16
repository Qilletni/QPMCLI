package dev.qilletni.qpm.cli.exceptions;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
