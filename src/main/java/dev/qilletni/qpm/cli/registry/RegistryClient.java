package dev.qilletni.qpm.cli.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qilletni.api.lib.qll.ComparableVersion;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.pkgutil.adapters.ComparableVersionTypeAdapter;
import dev.qilletni.pkgutil.adapters.VersionTypeAdapter;
import dev.qilletni.qpm.cli.adapters.VersionInfoTypeAdapter;
import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with the package registry API.
 */
public class RegistryClient {
    private static final Logger logger = LoggerFactory.getLogger(RegistryClient.class);
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ComparableVersion.class, new ComparableVersionTypeAdapter())
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .registerTypeAdapter(VersionInfo.class, new VersionInfoTypeAdapter())
            .create();

    private static final Duration METADATA_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);

    private final String registryUrl;
    private final HttpClient httpClient;

    public RegistryClient() {
        this.registryUrl = ConfigManager.getRegistryUrl();
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public RegistryClient(String registryUrl) {
        this.registryUrl = registryUrl;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Uploads a package to the registry.
     *
     * @param scope   the package scope (without @)
     * @param name    the package name
     * @param version the package version
     * @param file    the package file to upload
     * @param token   the GitHub authentication token
     * @return the upload response
     * @throws RegistryException if the upload fails
     * @throws IOException       if there's an error reading the file
     */
    public UploadResponse uploadPackage(String scope, String name, String version, Path file, String token)
        throws RegistryException, IOException {

        String url = String.format("%s/packages/%s/%s/%s", registryUrl, scope, name, version);
        byte[] fileBytes = Files.readAllBytes(file);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(DOWNLOAD_TIMEOUT)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/gzip")
//            .header("Content-Length", String.valueOf(fileBytes.length))
            .header("User-Agent", "qpm/1.0.0")
            .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println(response.body());
                return gson.fromJson(response.body(), UploadResponse.class);
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Upload failed with status: " + response.statusCode(), response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistryException("Upload interrupted", e);
        }
    }

    /**
     * Downloads a package from the registry.
     *
     * @param scope      the package scope (without @)
     * @param name       the package name
     * @param version    the package version
     * @param outputPath the path where the package should be saved
     * @throws RegistryException if the download fails
     * @throws IOException       if there's an error writing the file
     */
    public void downloadPackage(String scope, String name, String version, Path outputPath)
        throws RegistryException, IOException {

        String url = String.format("%s/packages/%s/%s/%s", registryUrl, scope, name, version);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(DOWNLOAD_TIMEOUT)
            .header("User-Agent", "qpm/1.0.0")
            .GET()
            .build();

        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(outputPath));

            if (response.statusCode() != 200) {
                // Delete partial file if download failed
                Files.deleteIfExists(outputPath);
                throw new RegistryException("Download failed with status: " + response.statusCode(), response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistryException("Download interrupted", e);
        }
    }

    /**
     * Gets metadata for a specific package version.
     *
     * @param scope   the package scope (without @)
     * @param name    the package name
     * @param version the package version
     * @return the package metadata
     * @throws RegistryException if the request fails
     */
    public PackageMetadata getPackageMetadata(String scope, String name, String version)
        throws RegistryException {

        String url = String.format("%s/packages/%s/%s/%s/metadata", registryUrl, scope, name, version);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(METADATA_TIMEOUT)
            .header("User-Agent", "qpm/1.0.0")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the response and convert uploadedAt to Instant
                Map<String, Object> jsonMap = gson.fromJson(response.body(), Map.class);
                String uploadedAtStr = (String) jsonMap.get("uploadedAt");
                Instant uploadedAt = uploadedAtStr != null ? Instant.parse(uploadedAtStr) : null;

                @SuppressWarnings("unchecked")
                Map<String, String> dependencies = (Map<String, String>) jsonMap.getOrDefault("dependencies", new HashMap<>());

                return new PackageMetadata(
                    (String) jsonMap.get("name"),
                    (String) jsonMap.get("version"),
                    (String) jsonMap.get("integrity"),
                    ((Number) jsonMap.get("size")).longValue(),
                    uploadedAt,
                    (String) jsonMap.get("uploadedBy"),
                    dependencies
                );
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Failed to get metadata, status: " + response.statusCode(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RegistryException("Failed to get package metadata", e);
        }
    }

    /**
     * Gets the list of versions for a package.
     *
     * @param scope the package scope (without @)
     * @param name  the package name
     * @return the package version list
     * @throws RegistryException if the request fails
     */
    public PackageVersionList getPackageVersions(String scope, String name)
        throws RegistryException {

        String url = String.format("%s/packages/%s/%s", registryUrl, scope, name);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(METADATA_TIMEOUT)
            .header("User-Agent", "qpm/1.0.0")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse using type adapter for cleaner code
                PackageVersionResponse apiResponse = gson.fromJson(response.body(), PackageVersionResponse.class);

                // Find the VersionInfo object that matches the latest version string
                VersionInfo latestInfo = apiResponse.versions.stream()
                    .filter(v -> v.version().equals(apiResponse.latest.version()))
                    .findFirst()
                    .orElseThrow(() -> new RegistryException("Latest version not found in versions list", 500));

                return new PackageVersionList(apiResponse.name, apiResponse.versions, latestInfo);
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Failed to get versions, status: " + response.statusCode(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RegistryException("Failed to get package versions", e);
        }
    }

    /**
     * Internal helper class for deserializing the API response.
     * The API returns "latest" as a string, which we convert to VersionInfo.
     */
    private static class PackageVersionResponse {
        String name;
        List<VersionInfo> versions;
        VersionInfo latest;
    }

    /**
     * Lists all packages in the registry.
     *
     * @return the package list response
     * @throws RegistryException if the request fails
     */
    public PackageListResponse listAllPackages() throws RegistryException {
        String url = registryUrl + "/packages";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(METADATA_TIMEOUT)
            .header("User-Agent", "qpm/1.0.0")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the response
                Map<String, Object> jsonMap = gson.fromJson(response.body(), Map.class);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> packagesData = (List<Map<String, Object>>) jsonMap.get("packages");
                int total = ((Number) jsonMap.get("total")).intValue();

                List<PackageListResponse.PackageInfo> packageInfoList = new ArrayList<>();
                for (Map<String, Object> pkgData : packagesData) {
                    String name = (String) pkgData.get("name");
                    String latest = (String) pkgData.get("latest");
                    int versionCount = ((Number) pkgData.get("versionCount")).intValue();

                    packageInfoList.add(new PackageListResponse.PackageInfo(name, latest, versionCount));
                }

                return new PackageListResponse(packageInfoList, total);
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Failed to list packages, status: " + response.statusCode(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RegistryException("Failed to list packages", e);
        }
    }

    /**
     * Deletes a specific version of a package.
     *
     * @param scope   the package scope (without @)
     * @param name    the package name
     * @param version the package version
     * @param token   the GitHub authentication token
     * @return the delete response
     * @throws RegistryException if the request fails
     */
    public DeleteResponse deletePackageVersion(String scope, String name, String version, String token) throws RegistryException {

        String url = String.format("%s/packages/%s/%s/%s", registryUrl, scope, name, version);
        System.out.println("url = " + url);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(METADATA_TIMEOUT)
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "qpm/1.0.0")
            .DELETE()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the response
                Map<String, Object> jsonMap = gson.fromJson(response.body(), Map.class);
                boolean success = (Boolean) jsonMap.get("success");

                @SuppressWarnings("unchecked")
                Map<String, Object> deletedData = (Map<String, Object>) jsonMap.get("deleted");
                String deletedName = (String) deletedData.get("name");
                String deletedVersion = (String) deletedData.get("version");
                DeleteResponse.DeletedInfo deletedInfo = new DeleteResponse.DeletedInfo(deletedName, deletedVersion);

                // Parse optional warning info
                DeleteResponse.WarningInfo warningInfo = null;
                if (jsonMap.containsKey("warning")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> warningData = (Map<String, Object>) jsonMap.get("warning");

                    @SuppressWarnings("unchecked")
                    List<String> dependents = (List<String>) warningData.get("dependents");
                    String message = (String) warningData.get("message");

                    warningInfo = new DeleteResponse.WarningInfo(dependents, message);
                }

                return new DeleteResponse(success, deletedInfo, warningInfo);
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Failed to delete package version, status: " + response.statusCode(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RegistryException("Failed to delete package version", e);
        }
    }

    /**
     * Requests a device code for OAuth authentication.
     *
     * @return the device code response
     * @throws RegistryException if the request fails
     */
    public DeviceCodeResponse requestDeviceCode() throws RegistryException {
        String url = registryUrl + "/auth/device/code";
        System.out.println("url = " + url);

        String requestBody = gson.toJson(Map.of("scope", "read:user read:org"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(METADATA_TIMEOUT)
            .header("Content-Type", "application/json")
            .header("User-Agent", "qpm/1.0.0")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DeviceCodeResponse.class);
            } else {
                handleErrorResponse(response);
                throw new RegistryException("Failed to request device code, status: " + response.statusCode(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RegistryException("Failed to request device code", e);
        }
    }

    /**
     * Handles error responses from the API.
     *
     * @param response the HTTP response
     * @throws RegistryException with appropriate error message
     */
    private void handleErrorResponse(HttpResponse<String> response) throws RegistryException {
        try {
            ErrorResponse errorResponse = gson.fromJson(response.body(), ErrorResponse.class);
            throw new RegistryException(errorResponse.message(), response.statusCode());
        } catch (Exception e) {
            // If we can't parse the error response, throw generic error
            logger.warn("Failed to parse error response: {}", response.body());
        }
    }

    public record PackageName(String scope, String name) {}

    /**
     * Parses package name into scope and name components.
     *
     * @param fullName the full package name (e.g., "@alice/postgres")
     * @return array with [scope, name] (without @ prefix)
     */
    public static PackageName parsePackageName(String fullName) {
        var splitName = fullName.split("/");

        if (splitName.length != 2) {
            return new PackageName("", splitName[1]);
        }

        return new PackageName(splitName[0], splitName[1]);
    }
}
