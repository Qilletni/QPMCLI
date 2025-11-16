package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.exceptions.IntegrityException;
import dev.qilletni.qpm.cli.integrity.IntegrityVerifier;
import dev.qilletni.qpm.cli.manifest.LockFile;
import dev.qilletni.qpm.cli.models.ResolvedPackage;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Verify command - verifies the integrity of installed packages.
 */
@Command(
    name = "verify",
    description = "Verify integrity of installed packages"
)
public class VerifyCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Override
    public Integer call() {
        try {
            // Step 1: Check if lock file exists
            Path lockFilePath = Paths.get("qilletni.lock");
            if (!Files.exists(lockFilePath)) {
                ProgressDisplay.error("Lock file not found: qilletni.lock");
                ProgressDisplay.error("Run 'qpm install' first to generate the lock file.");
                return 1;
            }

            // Step 2: Parse lock file
            ProgressDisplay.info("Reading lock file...");
            LockFile lockFile = LockFile.parse(lockFilePath);

            if (lockFile.getPackages().isEmpty()) {
                ProgressDisplay.info("No packages to verify.");
                return 0;
            }

            ProgressDisplay.info("Verifying " + lockFile.getPackages().size() + " packages...");
            ProgressDisplay.info("");

            // Step 3: Verify each package
            int verifiedCount = 0;
            int failedCount = 0;

            for (ResolvedPackage pkg : lockFile.getPackages().values()) {
                boolean verified = verifyPackage(pkg);
                if (verified) {
                    verifiedCount++;
                } else {
                    failedCount++;
                }
            }

            // Step 4: Display summary
            ProgressDisplay.info("");
            if (failedCount == 0) {
                ProgressDisplay.success("All packages verified successfully!");
                ProgressDisplay.info("Verified: " + verifiedCount + " packages");
                return 0;
            } else {
                ProgressDisplay.error("Verification failed!");
                ProgressDisplay.info("Verified: " + verifiedCount);
                ProgressDisplay.info("Failed: " + failedCount);
                return 1;
            }

        } catch (Exception e) {
            ProgressDisplay.error("Verification failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Verifies a single package.
     *
     * @param pkg the package to verify
     * @return true if verified successfully, false otherwise
     */
    private boolean verifyPackage(ResolvedPackage pkg) {
        try {
            // Build path to package file
            Path packagesDir = ConfigManager.getPackagesDir();
            String[] parts = pkg.name().split("/");
            if (parts.length != 2 || !parts[0].startsWith("@")) {
                ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - Invalid package name format");
                return false;
            }

            String scope = parts[0]; // Includes @
            String name = parts[1];
            Path packagePath = packagesDir.resolve(scope).resolve(name).resolve(pkg.version() + ".qll");

            // Check if file exists
            if (!Files.exists(packagePath)) {
                ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - File not found");
                return false;
            }

            // Verify integrity
            IntegrityVerifier.verifyIntegrity(packagePath, pkg.integrity());

            ProgressDisplay.success("✓ " + pkg.name() + "@" + pkg.version());
            return true;

        } catch (IntegrityException e) {
            ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - Integrity mismatch");
            ProgressDisplay.error("  Expected: " + e.getExpected());
            ProgressDisplay.error("  Actual:   " + e.getActual());
            return false;
        } catch (Exception e) {
            ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - " + e.getMessage());
            return false;
        }
    }
}
