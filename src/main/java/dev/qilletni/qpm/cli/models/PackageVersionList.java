package dev.qilletni.qpm.cli.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the package version list from the registry API.
 * Corresponds to the response from GET /packages/{scope}/{package}
 *
 * The backend now includes detailed version information with dependencies
 * to optimize dependency resolution by reducing API calls.
 */
public record PackageVersionList(
    String name,
    List<VersionInfo> versions,
    VersionInfo latest
) {
    public PackageVersionList {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Versions list cannot be null or empty");
        }
        if (latest == null) {
            throw new IllegalArgumentException("Latest version cannot be null");
        }
    }

    /**
     * Returns the latest version string for backward compatibility.
     */
    public String getLatestVersionString() {
        return latest.version();
    }

    /**
     * Returns a list of version strings for backward compatibility.
     */
    public List<String> getVersionStrings() {
        return versions.stream()
            .map(VersionInfo::version)
            .collect(Collectors.toList());
    }

    /**
     * Finds the VersionInfo for a specific version string.
     *
     * @param versionString the version to find
     * @return the VersionInfo or null if not found
     */
    public VersionInfo findVersion(String versionString) {
        return versions.stream()
            .filter(v -> v.version().equals(versionString))
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns a map of version strings to their VersionInfo objects.
     */
    public Map<String, VersionInfo> getVersionMap() {
        return versions.stream()
            .collect(Collectors.toMap(VersionInfo::version, v -> v));
    }
}
