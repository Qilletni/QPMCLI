package dev.qilletni.qpm.cli.models;

import java.util.List;

/**
 * Represents the response from DELETE /packages/{scope}/{package}/{version} endpoint.
 * Contains information about the deleted package and optional warnings about dependents.
 */
public record DeleteResponse(
    boolean success,
    DeletedInfo deleted,
    WarningInfo warning
) {
    /**
     * Information about the deleted package.
     */
    public record DeletedInfo(
        String name,
        String version
    ) {
        public DeletedInfo {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Package name cannot be null or empty");
            }
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("Version cannot be null or empty");
            }
        }
    }

    /**
     * Warning information about packages that depend on the deleted version.
     */
    public record WarningInfo(
        List<String> dependents,
        String message
    ) {
        public WarningInfo {
            if (dependents == null) {
                throw new IllegalArgumentException("Dependents list cannot be null");
            }
        }
    }
}
