package dev.qilletni.qpm.cli.manifest;

import dev.qilletni.qpm.cli.models.ResolvedPackage;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents and parses a qilletni.lock file.
 * Format:
 * version: 1
 * packages:
 *   "@alice/postgres@1.0.2":
 *     version: 1.0.2
 *     resolved: https://registry.../packages/@alice/postgres/1.0.2
 *     integrity: sha256-abc123...
 *     dependencies:
 *       "@bob/json": "^2.0.0"
 */
public class LockFile {
    private final int version;
    private final Map<String, ResolvedPackage> packages;

    public LockFile() {
        this.version = 1;
        this.packages = new LinkedHashMap<>();
    }

    public LockFile(int version, Map<String, ResolvedPackage> packages) {
        this.version = version;
        this.packages = packages != null ? packages : new LinkedHashMap<>();
    }

    public int getVersion() {
        return version;
    }

    public Map<String, ResolvedPackage> getPackages() {
        return packages;
    }

    /**
     * Adds a resolved package to the lock file.
     *
     * @param pkg the resolved package
     */
    public void addPackage(ResolvedPackage pkg) {
        packages.put(pkg.getFullIdentifier(), pkg);
    }

    /**
     * Parses a qilletni.lock file from the given path.
     *
     * @param lockFilePath the path to the qilletni.lock file
     * @return the parsed LockFile
     * @throws IOException if there's an error reading the file
     */
    public static LockFile parse(Path lockFilePath) throws IOException {
        if (!Files.exists(lockFilePath)) {
            throw new IOException("Lock file not found: " + lockFilePath);
        }

        String content = Files.readString(lockFilePath);
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(content);

        if (data == null) {
            throw new IOException("Lock file is empty or invalid");
        }

        int version = data.containsKey("version") ? ((Number) data.get("version")).intValue() : 1;

        Map<String, ResolvedPackage> packages = new LinkedHashMap<>();
        Object packagesObj = data.get("packages");

        if (packagesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> packagesMap = (Map<String, Map<String, Object>>) packagesObj;

            for (Map.Entry<String, Map<String, Object>> entry : packagesMap.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> pkgData = entry.getValue();

                String name = extractNameFromKey(key);
                String pkgVersion = (String) pkgData.get("version");
                String resolved = (String) pkgData.get("resolved");
                String integrity = (String) pkgData.get("integrity");

                @SuppressWarnings("unchecked")
                Map<String, String> dependencies = pkgData.containsKey("dependencies")
                    ? (Map<String, String>) pkgData.get("dependencies")
                    : new LinkedHashMap<>();

                ResolvedPackage pkg = new ResolvedPackage(name, pkgVersion, resolved, integrity, dependencies);
                packages.put(key, pkg);
            }
        }

        return new LockFile(version, packages);
    }

    /**
     * Writes the lock file to the given path.
     *
     * @param lockFilePath the path where to write the lock file
     * @throws IOException if there's an error writing the file
     */
    public void write(Path lockFilePath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", version);

        Map<String, Map<String, Object>> packagesMap = new LinkedHashMap<>();
        for (Map.Entry<String, ResolvedPackage> entry : packages.entrySet()) {
            ResolvedPackage pkg = entry.getValue();

            Map<String, Object> pkgData = new LinkedHashMap<>();
            pkgData.put("version", pkg.version());
            pkgData.put("resolved", pkg.resolved());
            pkgData.put("integrity", pkg.integrity());

            if (pkg.dependencies() != null && !pkg.dependencies().isEmpty()) {
                pkgData.put("dependencies", pkg.dependencies());
            } else {
                pkgData.put("dependencies", new LinkedHashMap<>());
            }

            packagesMap.put(entry.getKey(), pkgData);
        }

        data.put("packages", packagesMap);

        Yaml yaml = new Yaml();
        String content = yaml.dump(data);
        Files.writeString(lockFilePath, content);
    }

    /**
     * Extracts the package name from a lock file key (e.g., "@alice/postgres@1.0.2" -> "@alice/postgres").
     *
     * @param key the lock file key
     * @return the package name
     */
    private static String extractNameFromKey(String key) {
        int lastAtIndex = key.lastIndexOf('@');
        if (lastAtIndex > 0) {
            return key.substring(0, lastAtIndex);
        }
        return key;
    }

    @Override
    public String toString() {
        return "LockFile{" +
               "version=" + version +
               ", packages=" + packages.size() +
               '}';
    }
}
