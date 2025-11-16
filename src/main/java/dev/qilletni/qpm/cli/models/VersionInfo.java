package dev.qilletni.qpm.cli.models;

import java.time.Instant;
import java.util.Map;

/**
 * Represents detailed version information from the package index.
 * This includes all the metadata needed for dependency resolution without
 * requiring a separate metadata fetch.
 */
public record VersionInfo(
    String version,
    String integrity,
    long size,
    Instant uploadedAt,
    Map<String, String> dependencies
) {
    public VersionInfo {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        if (integrity == null || integrity.isEmpty()) {
            throw new IllegalArgumentException("Integrity cannot be null or empty");
        }
    }
}