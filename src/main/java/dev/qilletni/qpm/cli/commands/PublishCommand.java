package dev.qilletni.qpm.cli.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qilletni.api.lib.qll.ComparableVersion;
import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.pkgutil.adapters.ComparableVersionTypeAdapter;
import dev.qilletni.pkgutil.adapters.VersionTypeAdapter;
import dev.qilletni.qpm.cli.auth.AuthManager;
import dev.qilletni.qpm.cli.exceptions.AuthenticationException;
import dev.qilletni.qpm.cli.integrity.IntegrityVerifier;
import dev.qilletni.qpm.cli.models.UploadResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Publish command - uploads a .qll package to the registry.
 */
@Command(
    name = "publish",
    description = "Publish a package to the registry"
)
public class PublishCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the .qll package file")
    private String packageFile;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ComparableVersion.class, new ComparableVersionTypeAdapter())
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();

    @Override
    public Integer call() {
        try {
            // Step 1: Require authentication
            AuthManager.requireAuthentication();
            String token = AuthManager.getToken();

            // Step 2: Validate package file
            Path packagePath = Paths.get(packageFile);
            if (!Files.exists(packagePath)) {
                ProgressDisplay.error("Package file not found: " + packageFile);
                return 1;
            }

            if (!packageFile.endsWith(".qll")) {
                ProgressDisplay.error("Package file must have .qll extension");
                return 1;
            }

            ProgressDisplay.info("Reading package: " + packageFile);

            // Step 3: Extract and parse qilletni.info
            QllInfo qllInfo = extractQllInfo(packagePath);
            if (qllInfo == null) {
                ProgressDisplay.error("Failed to extract qilletni.info from package");
                return 1;
            }

            ProgressDisplay.info("Package: " + qllInfo.name() + " v" + qllInfo.version());

//             Step 4: Validate package name format
            if (!qllInfo.name().matches("^[a-zA-Z0-9-]+/[a-zA-Z0-9-]+$")) {
                ProgressDisplay.error("Invalid package name format: " + qllInfo.name());
                ProgressDisplay.error("Expected format: username/package-name");
                return 1;
            }

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
            ProgressDisplay.error(e.getMessage());
            return 1;
        } catch (Exception e) {
            ProgressDisplay.error("Failed to publish package: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Extracts and parses the qilletni.info file from a .qll package.
     *
     * @param packagePath the path to the .qll file
     * @return the parsed QllInfo, or null if extraction fails
     */
    private QllInfo extractQllInfo(Path packagePath) {
        try (ZipFile zipFile = new ZipFile(packagePath.toFile())) {
                ZipEntry infoEntry = zipFile.getEntry("qll.info");
            if (infoEntry == null) {
                ProgressDisplay.error("Package does not contain qll.info");
                return null;
            }

            try (InputStream is = zipFile.getInputStream(infoEntry)) {
                String json = new String(is.readAllBytes());
                return gson.fromJson(json, QllInfo.class);
            }
        } catch (IOException e) {
            ProgressDisplay.error("Failed to read package: " + e.getMessage());
            return null;
        } catch (Exception e) {
            ProgressDisplay.error("Failed to parse qll.info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
