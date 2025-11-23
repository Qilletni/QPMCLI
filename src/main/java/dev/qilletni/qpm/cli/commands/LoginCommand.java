package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.auth.AuthManager;
import dev.qilletni.qpm.cli.auth.DeviceFlowAuth;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;
import dev.qilletni.qpm.cli.models.DeviceCodeResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Login command - authenticates the user via GitHub OAuth device flow.
 * Requests 'read:user' and 'read:org' scopes for organization support.
 */
@Command(
    name = "login",
    description = "Authenticate with GitHub (includes organization permissions)"
)
public class LoginCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Override
    public Integer call() {
        try {
            // Check if already authenticated
            if (AuthManager.isAuthenticated()) {
                String username = getAuthenticatedUsername();
                if (username != null) {
                    ProgressDisplay.info("Already authenticated as: " + username);
                    ProgressDisplay.info("Run 'qpm logout' to sign out.");
                    return 0;
                }
            }

            ProgressDisplay.info("Initiating GitHub authentication...");

            // Create clients
            RegistryClient registryClient = new RegistryClient();
            DeviceFlowAuth deviceFlow = new DeviceFlowAuth(registryClient);

            // Step 1: Request device code
            DeviceCodeResponse deviceCode = deviceFlow.initiateDeviceFlow();

            // Step 2: Display instructions to user
            ProgressDisplay.info("");
            ProgressDisplay.info("Please visit: " + deviceCode.verification_uri());
            ProgressDisplay.info("And enter code: " + deviceCode.user_code());
            ProgressDisplay.info("");
            ProgressDisplay.info("Waiting for authentication...");

            // Step 3: Poll for token
            String token = deviceFlow.pollForToken(
                deviceCode.device_code(),
                deviceCode.interval(),
                deviceCode.expires_in()
            );

            // Step 4: Get username
            String username = deviceFlow.getAuthenticatedUser(token);

            // Step 5: Store token
            AuthManager.setToken(token);

            ProgressDisplay.success("Authenticated as: " + username);
            return 0;

        } catch (AuthenticationException e) {
            ProgressDisplay.error(e.getMessage());
            return 1;
        } catch (Exception e) {
            ProgressDisplay.error("Authentication failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Gets the currently authenticated username (if available).
     */
    private String getAuthenticatedUsername() {
        try {
            String token = AuthManager.getToken();
            RegistryClient registryClient = new RegistryClient();
            DeviceFlowAuth deviceFlow = new DeviceFlowAuth(registryClient);
            return deviceFlow.getAuthenticatedUser(token);
        } catch (Exception e) {
            return null;
        }
    }
}
