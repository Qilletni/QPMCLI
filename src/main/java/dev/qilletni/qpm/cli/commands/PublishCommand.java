package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.auth.AuthManager;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;
import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.models.UploadResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import dev.qilletni.qpm.cli.utils.PublishUtility;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Publish command - uploads a .qll package to the registry.
 * Supports publishing to personal namespaces (username/package) and
 * organization namespaces (orgname/package) if you are an admin.
 */
@Command(
    name = "publish",
    description = "Publish a package to the registry"
)
public class PublishCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = ".", description = "Path to the .qll package file")
    private String packageFile;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Override
    public Integer call() {
        try {
            // Step 1: Require authentication
            AuthManager.requireAuthentication();
            String token = AuthManager.getToken();

            var publishPrepOptional = PublishUtility.preparePublish(packageFile);
            if (publishPrepOptional.isEmpty()) {
                return 1;
            }

            var qllInfo = publishPrepOptional.get().qllInfo();
            var packagePath = publishPrepOptional.get().qllPath();

            ProgressDisplay.info("Package: " + qllInfo.scope() + "/" + qllInfo.name() + " v" + qllInfo.version().getVersionString());

            // Step 5: Compute integrity
            // TODO: Do something with this, check it?
//            ProgressDisplay.info("Computing integrity hash...");
//            String integrity = IntegrityVerifier.computeIntegrity(packagePath);

            // Step 6: Parse package name
            String version = qllInfo.version().getVersionString();

            // Step 7: Upload package
            ProgressDisplay.info("Uploading package...");
            RegistryClient registryClient = new RegistryClient();

            UploadResponse response = registryClient.uploadPackage(qllInfo.scope(), qllInfo.name(), version, packagePath, token);

            // Step 8: Display success
            ProgressDisplay.success("Package published successfully!");
            ProgressDisplay.info("Name: " + response.packageInfo().name());
            ProgressDisplay.info("Version: " + response.packageInfo().version());
            ProgressDisplay.info("Size: " + ProgressDisplay.formatBytes(response.packageInfo().size()));
            ProgressDisplay.info("Integrity: " + response.packageInfo().integrity());

            return 0;

        } catch (AuthenticationException e) {
            ProgressDisplay.error("Authentication failed", e);
            return 1;
        } catch (RegistryException e) {
            // Handle organization-specific errors
            if ("insufficient_permissions".equals(e.getErrorCode())) {
                ProgressDisplay.error("Cannot verify organization membership.");
                ProgressDisplay.error("");
                ProgressDisplay.error("The 'read:org' permission is required to publish to organization namespaces.");
                ProgressDisplay.error("Please re-authenticate: qpm login");
                return 1;
            } else if ("forbidden".equals(e.getErrorCode())) {
                // Display the detailed error message from the server
                // which includes org admin requirements and GitHub links
                ProgressDisplay.error("Failed to publish package:");
                ProgressDisplay.error(e.getMessage());
                return 1;
            } else {
                // Generic registry error
                ProgressDisplay.error("Failed to publish package: " + e.getMessage());
                return 1;
            }
        } catch (Exception e) {
            ProgressDisplay.error("Failed to publish package: " + e.getMessage(), e);
            return 1;
        }
    }
}
