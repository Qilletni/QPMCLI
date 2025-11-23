package dev.qilletni.qpm.cli.models;

/**
 * Represents an error response from the registry API.
 *
 * @param success Whether the operation succeeded (always false for errors)
 * @param error The error code (e.g., "forbidden", "insufficient_permissions")
 * @param message Human-readable error message with details
 */
public record ErrorResponse(
    boolean success,
    String error,
    String message
) {
    public ErrorResponse {
        if (error == null || error.isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
    }
}
