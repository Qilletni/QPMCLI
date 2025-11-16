package dev.qilletni.qpm.cli.models;

import java.util.List;

/**
 * Represents the response from GET /packages endpoint.
 * Contains a list of all packages in the registry with their metadata.
 */
public record PackageListResponse(
    List<PackageInfo> packages,
    int total
) {
    public PackageListResponse {
        if (packages == null) {
            throw new IllegalArgumentException("Packages list cannot be null");
        }
    }

    /**
     * Represents summary information for a single package.
     */
    public record PackageInfo(
        String name,
        String latest,
        int versionCount
    ) {
        public PackageInfo {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Package name cannot be null or empty");
            }
            if (latest == null || latest.isEmpty()) {
                throw new IllegalArgumentException("Latest version cannot be null or empty");
            }
        }
    }
}
