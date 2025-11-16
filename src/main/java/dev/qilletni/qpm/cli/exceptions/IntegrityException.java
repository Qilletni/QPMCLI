package dev.qilletni.qpm.cli.exceptions;

/**
 * Exception thrown when package integrity verification fails.
 */
public class IntegrityException extends Exception {
    private final String expected;
    private final String actual;

    public IntegrityException(String message, String expected, String actual) {
        super(message);
        this.expected = expected;
        this.actual = actual;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }
}
