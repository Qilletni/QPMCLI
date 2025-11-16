package dev.qilletni.qpm.cli.models;

import java.util.Map;

/**
 * Represents a resolved package with its dependencies.
 * Used in the lock file and dependency resolution.
 */
public record ResolvedPackage(
    String name,
    String version,
    String resolved,
    String integrity,
    Map<String, String> dependencies
) {
    public ResolvedPackage {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Package version cannot be null or empty");
        }
        if (resolved == null || resolved.isEmpty()) {
            throw new IllegalArgumentException("Package resolved URL cannot be null or empty");
        }
        if (integrity == null || integrity.isEmpty()) {
            throw new IllegalArgumentException("Package integrity cannot be null or empty");
        }
    }

    /**
     * Returns the full package identifier (name@version).
     */
    public String getFullIdentifier() {
        return name + "@" + version;
    }
}
