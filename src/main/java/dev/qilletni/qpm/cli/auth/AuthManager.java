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
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return ConfigManager.hasToken();
    }

    /**
     * Gets the current authentication token.
     *
     * @return the token
     * @throws AuthenticationException if not authenticated
     */
    public static String getToken() throws AuthenticationException {
        String token = ConfigManager.getToken();
        if (token == null || token.isEmpty()) {
            throw new AuthenticationException("Not authenticated. Please run 'qpm login' first.");
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
