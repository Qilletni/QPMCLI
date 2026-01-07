package dev.qilletni.qpm.cli.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qilletni.api.lib.qll.ComparableVersion;
import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.pkgutil.adapters.ComparableVersionTypeAdapter;
import dev.qilletni.pkgutil.adapters.VersionTypeAdapter;
import dev.qilletni.qpm.cli.manifest.Manifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PublishUtility {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ComparableVersion.class, new ComparableVersionTypeAdapter())
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();

    public static Optional<PublishPreparation> preparePublish(String packageFile) throws IOException {
        return PublishUtility.findLatestQll(packageFile)
                .flatMap(packagePath -> {
                    var qllInfo = extractQllInfo(packagePath);
                    if (qllInfo == null) {
                        ProgressDisplay.error("Failed to extract qilletni.info from package");
                        return Optional.empty();
                    }

                    if (qllInfo.scope() == null || qllInfo.scope().isEmpty()) {
                        ProgressDisplay.error("Package does not contain a scope.");
                        return Optional.empty();
                    }

                    return Optional.of(new PublishPreparation(packagePath, qllInfo));
                });
    }

    /**
     * Gets the latest `.qll` to package from the given commandline parameter. If the param is `"."`, it will auto
     * detect based on the `qilletni_info.yml`. Otherwise, an exact path will be used.
     *
     * @param packageFile The commandline param. Either `"."` or a `.qll` path
     * @return The found `.qll` file, if any. An empty Optional indicates no file found.
     * @throws IOException
     */
    public static Optional<Path> findLatestQll(String packageFile) throws IOException {
        ProgressDisplay.info("Reading package: " + packageFile);

        Path packagePath;
        if (packageFile.equals(".")) {
            Path manifestPath = Paths.get("qilletni_info.yml");
            var qilletniSrc = Paths.get("qilletni-src");

            if (!Files.exists(manifestPath) && Files.exists(qilletniSrc)) {
                manifestPath = qilletniSrc.resolve(manifestPath);
            }

            if (!Files.exists(manifestPath)) {
                ProgressDisplay.error("Unable to find qilletni_info.yml to identify output file. Try specifying an exact path, or running this in a project root.");
                return Optional.empty();
            }

            var manifest = Manifest.parse(manifestPath);

            // Remove scope from name
            var packageParts = manifest.name().split("/");
            var packageName = packageParts[packageParts.length - 1];
            packagePath = Paths.get("build", "ql-build", "%s-%s.qll".formatted(packageName,  manifest.version()));

            if (!Files.exists(packagePath)) {
                ProgressDisplay.error("Can't find expected latest build at: %s".formatted(packagePath.toAbsolutePath()));
                return Optional.empty();
            }
        } else {
            packagePath = Paths.get(packageFile);
            if (!Files.exists(packagePath)) {
                ProgressDisplay.error("Package file not found: %s".formatted(packageFile));
                return Optional.empty();
            }

            if (!packageFile.endsWith(".qll")) {
                ProgressDisplay.error("Package file must have .qll extension");
                return Optional.empty();
            }
        }

        return Optional.of(packagePath);
    }

    /**
     * Extracts and parses the qilletni.info file from a .qll package.
     *
     * @param packagePath the path to the .qll file
     * @return the parsed QllInfo, or null if extraction fails
     */
    private static QllInfo extractQllInfo(Path packagePath) {
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

    public record PublishPreparation(Path qllPath, QllInfo qllInfo) {}

}
