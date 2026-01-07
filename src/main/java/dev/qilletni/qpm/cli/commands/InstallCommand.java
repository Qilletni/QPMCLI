package dev.qilletni.qpm.cli.commands;

import dev.qilletni.pkgutil.manifest.LockFile;
import dev.qilletni.pkgutil.manifest.models.ResolvedPackage;
import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.exceptions.IntegrityException;
import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.integrity.IntegrityVerifier;
import dev.qilletni.qpm.cli.manifest.DependencyResolver;
import dev.qilletni.qpm.cli.manifest.Manifest;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
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
            var manifestPath = Paths.get("qilletni_info.yml");
            var lockFilePath = Paths.get("qilletni.lock");
            var qilletniSrc = Paths.get("qilletni-src");

            if (!Files.exists(manifestPath) && Files.exists(qilletniSrc)) {
                manifestPath = qilletniSrc.resolve(manifestPath);
                lockFilePath = qilletniSrc.resolve(lockFilePath);
            }

            // Step 1: Check if manifest exists
            if (!Files.exists(manifestPath)) {
                ProgressDisplay.error("qilletni_info.yml not found");
                return 1;
            }

            // Step 2: Check if lock file exists
            LockFile lockFile;
            if (!Files.exists(lockFilePath)) {
                ProgressDisplay.info("Lock file not found, resolving dependencies...");

                // Parse manifest
                var manifest = Manifest.parse(manifestPath);
                ProgressDisplay.info("Resolving dependencies for " + manifest.name() + "...");

                // Resolve dependencies
                var resolver = new DependencyResolver(registryClient);
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
            ProgressDisplay.error("Installation failed: " + e.getMessage(), e);
            return 1;
        }
    }

    /**
     * Checks if a given package has a locally-published copy.
     * TODO: Currently this does not check for compatible package versions, only exact matches. Should this be changed?
     *
     * @param scope The scope of the package.
     * @param name The name of the package.
     * @param version The version of the package.
     * @return If a locally published copy exists.
     * @throws IOException
     */
    private boolean hasLocalVariant(String scope, String name, String version) throws IOException {
        var packagesDir = ConfigManager.getLocalPackagesDir();
        var packageDir = packagesDir.resolve(scope).resolve(name);
        Files.createDirectories(packageDir);

        var packagePath = packageDir.resolve("%s-%s.qll".formatted(name, version));

        return Files.exists(packagePath);
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

            if (hasLocalVariant(scope, name, pkg.version())) {
                ProgressDisplay.success(pkg.name() + "@" + pkg.version() + " (local version found)");
                installedCount++;
                return;
            }

            // Build package path
            var packagesDir = ConfigManager.getPackagesDir();
            var packageDir = packagesDir.resolve(scope).resolve(name);
            Files.createDirectories(packageDir);

            var packagePath = packageDir.resolve("%s-%s.qll".formatted(name, pkg.version()));

            // Check if already installed and verified
            if (Files.exists(packagePath)) {
                try {
                    IntegrityVerifier.verifyIntegrity(packagePath, pkg.integrity());
                    ProgressDisplay.success(pkg.name() + "@" + pkg.version() + " (already installed)");
                    installedCount++;
                    return;
                } catch (IntegrityException | IOException e) {
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

        } catch (IntegrityException | IOException | RegistryException e) {
            ProgressDisplay.error("✗ " + pkg.name() + "@" + pkg.version() + " - " + e.getMessage());
        }
    }
}
