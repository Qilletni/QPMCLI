package dev.qilletni.qpm.cli.models;

/**
 * Represents the response from a package upload.
 * Corresponds to the response from POST /packages/{scope}/{package}/{version}
 */
public record UploadResponse(
    boolean success,
    PackageInfo packageInfo
) {
    public record PackageInfo(
        String name,
        String version,
        String integrity,
        long size
    ) {}
}
