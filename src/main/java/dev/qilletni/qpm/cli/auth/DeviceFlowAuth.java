package dev.qilletni.qpm.cli.auth;

import com.google.gson.Gson;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;
import dev.qilletni.qpm.cli.models.DeviceCodeResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Handles GitHub OAuth device flow authentication.
 */
public class DeviceFlowAuth {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceFlowAuth.class);
    private static final Gson gson = new Gson();
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_CLIENT_ID = System.getenv("GITHUB_CLIENT_ID");
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);

    private final RegistryClient registryClient;
    private final HttpClient httpClient;

    public DeviceFlowAuth(RegistryClient registryClient) {
        this.registryClient = registryClient;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Initiates the device flow authentication process.
     *
     * @return the device code response containing user code and verification URL
     * @throws AuthenticationException if the request fails
     */
    public DeviceCodeResponse initiateDeviceFlow() throws AuthenticationException {
        try {
            return registryClient.requestDeviceCode();
        } catch (Exception e) {
            throw new AuthenticationException("Failed to initiate device flow", e);
        }
    }

    /**
     * Polls GitHub for an access token.
     *
     * @param deviceCode the device code from initiateDeviceFlow
     * @param interval   the polling interval in seconds
     * @param expiresIn  the expiration time in seconds
     * @return the access token
     * @throws AuthenticationException if authentication fails or times out
     */
    public String pollForToken(String deviceCode, int interval, int expiresIn) throws AuthenticationException {
        long startTime = System.currentTimeMillis();
        long expirationTime = startTime + (expiresIn * 1000L);
        int pollInterval = interval * 1000; // Convert to milliseconds

        while (System.currentTimeMillis() < expirationTime) {
            try {
                String result = checkDeviceCode(deviceCode);

                if (result != null) {
                    return result; // Successfully got token
                }

                // Wait before polling again
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AuthenticationException("Authentication interrupted", e);
            } catch (AuthenticationException e) {
                throw e; // Re-throw authentication exceptions
            } catch (Exception e) {
                LOGGER.warn("Error polling for token: {}", e.getMessage());
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AuthenticationException("Authentication interrupted", ie);
                }
            }
        }

        throw new AuthenticationException("Authentication timed out. Please try again.");
    }

    /**
     * Checks the device code status with GitHub.
     *
     * @param deviceCode the device code
     * @return the access token if authorized, null if still pending
     * @throws AuthenticationException if there's an error or the code is invalid
     */
    private String checkDeviceCode(String deviceCode) throws AuthenticationException {
        String requestBody = String.format(
            "client_id=%s&device_code=%s&grant_type=urn:ietf:params:oauth:grant-type:device_code",
            GITHUB_CLIENT_ID, deviceCode
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_TOKEN_URL))
            .timeout(POLL_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = gson.fromJson(response.body(), Map.class);

            // Check for error
            if (result.containsKey("error")) {
                String error = (String) result.get("error");

                return switch (error) {
                    case "authorization_pending" ->
                        // Still waiting for user authorization
                            null;
                    case "slow_down" -> {
                        // Polling too fast, wait longer
                        LOGGER.debug("Polling too fast, slowing down");
                        yield null;
                    }
                    case "expired_token" ->
                            throw new AuthenticationException("Device code has expired. Please try again.");
                    case "access_denied" -> throw new AuthenticationException("Access denied by user.");
                    default -> throw new AuthenticationException("Authentication error: " + error);
                };
            }

            // Successfully got token
            if (result.containsKey("access_token")) {
                return (String) result.get("access_token");
            }

            throw new AuthenticationException("Unexpected response from GitHub");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AuthenticationException("Failed to check device code", e);
        }
    }

    /**
     * Gets the authenticated user's GitHub username.
     *
     * @param token the access token
     * @return the username
     * @throws AuthenticationException if the request fails
     */
    public String getAuthenticatedUser(String token) throws AuthenticationException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/user"))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .header("User-Agent", "qpm/1.0.0")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> user = gson.fromJson(response.body(), Map.class);
                return (String) user.get("login");
            } else {
                throw new AuthenticationException("Failed to get user info: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AuthenticationException("Failed to get authenticated user", e);
        }
    }
}
