package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.auth.AuthManager;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;
import dev.qilletni.qpm.cli.models.DeleteResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ColorSupport;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Delete command - deletes a specific version of a package.
 * Requires authentication and ownership of the package.
 */
@Command(
    name = "delete",
    description = "Delete a specific version of a package"
)
public class DeleteCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Option(names = {"--force", "-f"}, description = "Skip confirmation prompt")
    private boolean force;

    @Parameters(index = "0", description = "Package scope (without @)")
    private String scope;

    @Parameters(index = "1", description = "Package name")
    private String packageName;

    @Parameters(index = "2", description = "Version to delete")
    private String version;

    private final RegistryClient registryClient;

    public DeleteCommand() {
        this.registryClient = new RegistryClient();
    }

    @Override
    public Integer call() {
        try {
            // Require authentication
            AuthManager.requireAuthentication();
            String token = AuthManager.getToken();

            // Build full package name for display
            String fullName = scope + "/" + packageName;

            // Confirmation prompt (unless --force is used)
            if (!force) {
                if (!confirmDeletion(fullName, version)) {
                    ProgressDisplay.info("Deletion cancelled.");
                    return 0;
                }
            }

            // Perform deletion
            ProgressDisplay.info("Deleting " + fullName + "@" + version + "...");
            DeleteResponse response = registryClient.deletePackageVersion(scope, packageName, version, token);

            if (response.success()) {
                ProgressDisplay.success("Successfully deleted " + ColorSupport.bold(fullName) + "@" + ColorSupport.cyan(version));

                // Display warning if there are dependents
                if (response.warning() != null) {
                    System.out.println();
                    ProgressDisplay.warn(response.warning().message());

                    if (!response.warning().dependents().isEmpty()) {
                        System.out.println("Affected packages:");
                        for (String dependent : response.warning().dependents()) {
                            System.out.println("  - " + ColorSupport.cyan(dependent));
                        }
                    }
                }
            } else {
                ProgressDisplay.error("Deletion failed");
                return 1;
            }

            return 0;

        } catch (AuthenticationException e) {
            ProgressDisplay.error("Authentication required. Please run 'qpm login' first.");
            return 1;
        } catch (Exception e) {
            ProgressDisplay.error("Failed to delete package: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Prompts the user for confirmation before deletion with color-highlighted warnings.
     *
     * @param packageName the full package name
     * @param version     the version to delete
     * @return true if user confirms, false otherwise
     */
    private boolean confirmDeletion(String packageName, String version) {
        System.out.println();
        System.out.println(ColorSupport.brightYellow("⚠️  WARNING: This action cannot be undone!"));
        System.out.println();
        System.out.print("Delete " + ColorSupport.bold(packageName) + " " + ColorSupport.cyan(version) + "? (y/N): ");

        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine().trim().toLowerCase();

        return response.equals("y") || response.equals("yes");
    }
}
