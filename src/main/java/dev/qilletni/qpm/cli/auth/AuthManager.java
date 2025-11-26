package dev.qilletni.qpm.cli.auth;

import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;

import java.io.IOException;

/**
 * Manages authentication state for the CLI.
 * Provides convenience methods for checking authentication and managing tokens.
 */
public class AuthManager {

    /**
     * Checks if the user is authenticated (has a valid token).
     * Checks environment variables (QPM_TOKEN, GITHUB_TOKEN) before config file.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        // Check environment variables first
        String envToken = System.getenv("QPM_TOKEN");
        if (envToken == null || envToken.isEmpty()) {
            envToken = System.getenv("GITHUB_TOKEN");
        }

        if (envToken != null && !envToken.isEmpty()) {
            return true;
        }

        // Fall back to config file
        return ConfigManager.hasToken();
    }

    /**
     * Gets the current authentication token.
     * Checks environment variables (QPM_TOKEN, GITHUB_TOKEN) before config file.
     *
     * @return the token
     * @throws AuthenticationException if not authenticated
     */
    public static String getToken() throws AuthenticationException {
        String envToken = System.getenv("QPM_TOKEN");
        if (envToken == null || envToken.isEmpty()) {
            envToken = System.getenv("GITHUB_TOKEN");
        }

        if (envToken != null && !envToken.isEmpty()) {
            return envToken;
        }

        // Priority 2: Fall back to stored token (for interactive usage)
        String token = ConfigManager.getToken();
        if (token == null || token.isEmpty()) {
            throw new AuthenticationException(
                    """
                    Not authenticated. Please either:
                      - Run 'qpm login' for interactive authentication, or
                      - Set QPM_TOKEN or GITHUB_TOKEN environment variable for CI/CD usage"""
            );
        }
        return token;
    }

    /**
     * Stores an authentication token.
     *
     * @param token the token to store
     * @throws IOException if there's an error saving the token
     */
    public static void setToken(String token) throws IOException {
        ConfigManager.updateToken(token);
    }

    /**
     * Clears the authentication token (logout).
     *
     * @throws IOException if there's an error clearing the token
     */
    public static void clearToken() throws IOException {
        ConfigManager.clearToken();
    }

    /**
     * Requires authentication, throwing an exception if not authenticated.
     *
     * @throws AuthenticationException if not authenticated
     */
    public static void requireAuthentication() throws AuthenticationException {
        if (!isAuthenticated()) {
            throw new AuthenticationException("Authentication required. Please run 'qpm login' first.");
        }
    }
}
