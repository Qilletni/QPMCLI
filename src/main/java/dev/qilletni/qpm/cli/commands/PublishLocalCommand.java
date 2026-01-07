package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import dev.qilletni.qpm.cli.utils.PublishUtility;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * Publish command - uploads a .qll package to the registry.
 * Supports publishing to personal namespaces (username/package) and
 * organization namespaces (orgname/package) if you are an admin.
 */
@Command(
    name = "publish-local",
    description = "Publish a package to the registry"
)
public class PublishLocalCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = ".", description = "Path to the .qll package file")
    private String packageFile;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Override
    public Integer call() {
        try {
            ProgressDisplay.info("Publishing local package...");

            var publishPrepOptional = PublishUtility.preparePublish(packageFile);
            if (publishPrepOptional.isEmpty()) {
                return 1;
            }

            var qllInfo = publishPrepOptional.get().qllInfo();
            var publishingPackagePath = publishPrepOptional.get().qllPath();

            ProgressDisplay.info("Package: " + qllInfo.scope() + "/" + qllInfo.name() + " v" + qllInfo.version().getVersionString());

            // Step 6: Parse package name
            String version = qllInfo.version().getVersionString();

            ConfigManager.ensurePackagesDir();

            var localPackagesDir = ConfigManager.getLocalPackagesDir();
            var packageDir = localPackagesDir.resolve(qllInfo.scope()).resolve(qllInfo.name());
            Files.createDirectories(packageDir);

            var packagePath = packageDir.resolve("%s-%s.qll".formatted(qllInfo.name(), version));

            Files.copy(publishingPackagePath, packagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ProgressDisplay.success("Local package published successfully!");
            ProgressDisplay.info("Source: %s".formatted(publishingPackagePath.toAbsolutePath()));
            ProgressDisplay.info("Destination: %s".formatted(packagePath.toAbsolutePath()));

            return 0;
        } catch (Exception e) {
            ProgressDisplay.error("Failed to publish package: " + e.getMessage(), e);
            return 1;
        }
    }
}
