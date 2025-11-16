package dev.qilletni.qpm.cli.models;

/**
 * Represents the response from initiating OAuth device flow.
 * Corresponds to the response from POST /auth/device/code
 */
public record DeviceCodeResponse(
    String device_code,
    String user_code,
    String verification_uri,
    int expires_in,
    int interval
) {
    public DeviceCodeResponse {
        if (device_code == null || device_code.isEmpty()) {
            throw new IllegalArgumentException("Device code cannot be null or empty");
        }
        if (user_code == null || user_code.isEmpty()) {
            throw new IllegalArgumentException("User code cannot be null or empty");
        }
        if (verification_uri == null || verification_uri.isEmpty()) {
            throw new IllegalArgumentException("Verification URI cannot be null or empty");
        }
    }
}
