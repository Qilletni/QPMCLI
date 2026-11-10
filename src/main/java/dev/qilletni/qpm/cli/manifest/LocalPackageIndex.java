package dev.qilletni.qpm.cli.manifest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qilletni.qpm.cli.integrity.IntegrityVerifier;
import dev.qilletni.qpm.cli.models.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Lists the versions of packages published locally via {@code qpm publish-local}, so they can be used during
 * dependency resolution alongside the registry.
 */
public class LocalPackageIndex {
    private static final Logger logger = LoggerFactory.getLogger(LocalPackageIndex.class);

    private final Path localPackagesDir;

    public LocalPackageIndex(Path localPackagesDir) {
        this.localPackagesDir = localPackagesDir;
    }

    /**
     * Gets all locally published versions of a package.
     *
     * @param scope The scope of the package
     * @param name  The name of the package
     * @return The version info of every readable local .qll for the package, empty if none
     */
    public List<VersionInfo> getVersions(String scope, String name) {
        var packageDir = localPackagesDir.resolve(scope).resolve(name);
        if (!Files.isDirectory(packageDir)) {
            return List.of();
        }

        var prefix = name + "-";
        var versions = new ArrayList<VersionInfo>();

        try (Stream<Path> files = Files.list(packageDir)) {
            for (var file : files.toList()) {
                var fileName = file.getFileName().toString();
                if (!Files.isRegularFile(file) || !fileName.startsWith(prefix) || !fileName.endsWith(".qll")) {
                    continue;
                }

                try {
                    versions.add(readVersionInfo(file));
                } catch (IOException | RuntimeException e) {
                    logger.warn("Skipping unreadable local package {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to list local packages in {}: {}", packageDir, e.getMessage());
        }

        return versions;
    }

    private VersionInfo readVersionInfo(Path qllFile) throws IOException {
        JsonObject info;
        try (var zipFile = new ZipFile(qllFile.toFile())) {
            var infoEntry = zipFile.getEntry("qll.info");
            if (infoEntry == null) {
                throw new IOException("Package does not contain qll.info");
            }

            try (InputStream is = zipFile.getInputStream(infoEntry)) {
                info = JsonParser.parseString(new String(is.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }

        var version = info.get("version").getAsString();

        Map<String, String> dependencies = new LinkedHashMap<>();
        var dependenciesElement = info.get("dependencies");
        if (dependenciesElement != null && dependenciesElement.isJsonArray()) {
            for (JsonElement depElement : dependenciesElement.getAsJsonArray()) {
                var dep = depElement.getAsJsonObject();
                dependencies.put(dep.get("scope").getAsString() + "/" + dep.get("name").getAsString(), dep.get("version").getAsString());
            }
        }

        return new VersionInfo(
            version,
            IntegrityVerifier.computeIntegrity(qllFile),
            Files.size(qllFile),
            Files.getLastModifiedTime(qllFile).toInstant(),
            dependencies
        );
    }
}