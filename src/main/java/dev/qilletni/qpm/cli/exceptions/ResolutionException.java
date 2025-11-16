package dev.qilletni.qpm.cli.exceptions;

/**
 * Exception thrown when dependency resolution fails (conflicts, missing packages, etc.).
 */
public class ResolutionException extends Exception {
    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
