package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.integrity.IntegrityVerifier;
import dev.qilletni.qpm.cli.manifest.DependencyResolver;
import dev.qilletni.qpm.cli.manifest.LockFile;
import dev.qilletni.qpm.cli.manifest.Manifest;
import dev.qilletni.qpm.cli.models.ResolvedPackage;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Install command - installs packages from manifest and lock file.
 */
@Command(
    name = "install",
    description = "Install packages from manifest and lock file"
)
public class InstallCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    private final RegistryClient registryClient;
    private int installedCount = 0;

    public InstallCommand() {
        this.registryClient = new RegistryClient();
    }

    @Override
    public Integer call() {
        try {
            Path manifestPath = Paths.get("qilletni_info.yml");
            Path lockFilePath = Paths.get("qilletni.lock");

            // Step 1: Check if manifest exists
            if (!Files.exists(manifestPath)) {
                ProgressDisplay.error("Manifest file not found: manifest.dsl");
                return 1;
            }

            // Step 2: Check if lock file exists
            LockFile lockFile;
            if (!Files.exists(lockFilePath)) {
                ProgressDisplay.info("Lock file not found, resolving dependencies...");

                // Parse manifest
                Manifest manifest = Manifest.parse(manifestPath);
                ProgressDisplay.info("Resolving dependencies for " + manifest.name() + "...");

                // Resolve dependencies
                DependencyResolver resolver = new DependencyResolver(registryClient);
                lockFile = resolver.resolve(manifest);

                // Write lock file
                lockFile.write(lockFilePath);
                ProgressDisplay.success("Lock file created with " + lockFile.getPackages().size() + " packages");
            } else {
                ProgressDisplay.info("Reading lock file...");
                lockFile = LockFile.parse(lockFilePath);
            }

            // Step 3: Ensure packages directory exists
            ConfigManager.ensurePackagesDir();

            // Step 4: Install packages
            if (lockFile.getPackages().isEmpty()) {
                ProgressDisplay.info("No packages to install.");
                return 0;
            }

            ProgressDisplay.info("Installing " + lockFile.getPackages().size() + " packages...");
            ProgressDisplay.info("");

            for (ResolvedPackage pkg : lockFile.getPackages().values()) {
                installPackage(pkg);
            }

            // Step 5: Display summary
            ProgressDisplay.info("");
            ProgressDisplay.success("Installation complete!");
            ProgressDisplay.info("Installed " + installedCount + " package(s)");

            return 0;

        } catch (Exception e) {
            ProgressDisplay.error("Installation failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Installs a single package.
     *
     * @param pkg the package to install
     */
    private void installPackage(ResolvedPackage pkg) {
        try {
            String[] parts = pkg.name().split("/");
            if (parts.length != 2) {
                ProgressDisplay.error("Invalid package name format: " + pkg.name());
                return;
            }

            String scope = parts[0];
            String name = parts[1];

            // Build package path
            Path packagesDir = ConfigManager.getPackagesDir();
            Path packageDir = packagesDir.resolve("@" + scope).resolve(name);
            Files.createDirectories(packageDir);

            Path packagePath = packageDir.resolve(pkg.version() + ".qll");

            // Check if already installed and verified
            if (Files.exists(packagePath)) {
                try {
                    IntegrityVerifier.verifyIntegrity(packagePath, pkg.integrity());
                    ProgressDisplay.success(pkg.name() + "@" + pkg.version() + " (already installed)");
                    installedCount++;
                    return;
                } catch (Exception e) {
                    // Integrity check failed, re-download
                    ProgressDisplay.warn(pkg.name() + "@" + pkg.version() + " - integrity check failed, re-downloading");
                    Files.deleteIfExists(packagePath);
                }
            }

            // Download package
            ProgressDisplay.info("Downloading " + pkg.name() + "@" + pkg.version() + "...");
            registryClient.downloadPackage(scope, name, pkg.version(), packagePath);

            // Verify integrity
            IntegrityVerifier.verifyIntegrity(packagePath, pkg.integrity());

            ProgressDisplay.success(pkg.name() + "@" + pkg.version());
            installedCount++;

        } catch (Exception e) {
            ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - " + e.getMessage());
        }
    }
}
