package dev.qilletni.qpm.cli.models;

import java.time.Instant;
import java.util.Map;

/**
 * Represents package metadata from the registry API.
 * Corresponds to the response from GET /packages/{scope}/{package}/{version}/metadata
 */
public record PackageMetadata(
    String name,
    String version,
    String integrity,
    long size,
    Instant uploadedAt,
    String uploadedBy,
    Map<String, String> dependencies
) {
    public PackageMetadata {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Package version cannot be null or empty");
        }
        if (integrity == null || integrity.isEmpty()) {
            throw new IllegalArgumentException("Package integrity cannot be null or empty");
        }
    }
}
