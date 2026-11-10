package dev.qilletni.qpm.cli.manifest;

import dev.qilletni.pkgutil.manifest.LockFile;
import dev.qilletni.pkgutil.manifest.models.ResolvedPackage;
import dev.qilletni.qpm.cli.config.ConfigManager;
import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.exceptions.ResolutionException;
import dev.qilletni.qpm.cli.models.*;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.SemverUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Resolves package dependencies using a breadth-first algorithm.
 * Implements simple one-version-per-package resolution with conflict detection.
 */
public class DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);

    private final RegistryClient registryClient;
    private final LocalPackageIndex localPackageIndex;
    private final Set<String> localVersionStrings = new HashSet<>();

    public DependencyResolver(RegistryClient registryClient) {
        this(registryClient, ConfigManager.getLocalPackagesDir());
    }

    public DependencyResolver(RegistryClient registryClient, Path localPackagesDir) {
        this.registryClient = registryClient;
        this.localPackageIndex = new LocalPackageIndex(localPackagesDir);
    }

    /**
     * Resolves dependencies from a manifest file.
     *
     * @param manifest the manifest containing dependencies
     * @return a LockFile with resolved dependencies
     * @throws ResolutionException if resolution fails
     */
    public LockFile resolve(Manifest manifest) throws ResolutionException {
        logger.info("Resolving dependencies for {}", manifest.name());

        Map<String, ResolvedPackage> resolved = new LinkedHashMap<>();
        Queue<DependencySpec> queue = new LinkedList<>(manifest.dependencies());

        while (!queue.isEmpty()) {
            DependencySpec depSpec = queue.poll();
            String packageName = depSpec.packageName();
            String constraint = depSpec.versionConstraint();

            logger.debug("Processing dependency: {} with constraint {}", packageName, constraint);

            // Check if already resolved
            if (isAlreadyResolved(resolved, packageName)) {
                // Check if the resolved version satisfies the current constraint
                ResolvedPackage existingPkg = findResolvedPackage(resolved, packageName);
                Version existingVersion = Version.parse(existingPkg.version());

                if (!SemverUtility.satisfies(existingVersion, constraint)) {
                    throw new ResolutionException(String.format(
                        "Conflict: %s requires version %s but %s is already resolved",
                        packageName, constraint, existingVersion
                    ));
                }

                logger.debug("Package {} already resolved with compatible version {}", packageName, existingVersion);
                continue;
            }

            // Fetch all available versions (registry + locally published)
            Map<String, VersionInfo> versionMap = fetchAvailableVersions(packageName);

            // Get available version strings
            List<String> availableVersions = new ArrayList<>(versionMap.keySet());

            // Find best matching version
            Version bestVersion = SemverUtility.findMaxSatisfying(availableVersions, constraint);
            if (bestVersion == null) {
                throw new ResolutionException(String.format(
                    "No version of %s satisfies constraint: %s (available: %s)",
                    packageName, constraint, String.join(", ", availableVersions)
                ));
            }

            // Get the version info from the index (already fetched)
            VersionInfo versionInfo = versionMap.get(bestVersion.toString());
            boolean isLocal = versionInfo != null && localVersionStrings.contains(packageName + "@" + bestVersion);
            logger.info("Resolved {} to version {}{}", packageName, bestVersion, isLocal ? " (local)" : "");
            if (versionInfo == null) {
                throw new ResolutionException("Version info not found for " + packageName + "@" + bestVersion);
            }

            // Create resolved package using data from the index
            var packageNameScope = RegistryClient.parsePackageName(packageName);
            String resolvedUrl = String.format("%s/%s/%s-%s", packageNameScope.scope(), packageNameScope.name(), packageNameScope.name(), bestVersion);

            ResolvedPackage resolvedPkg = new ResolvedPackage(
                packageName,
                bestVersion.toString(),
                resolvedUrl,
                versionInfo.integrity(),
                versionInfo.dependencies()
            );

            resolved.put(packageName + "@" + bestVersion, resolvedPkg);

            // Add transitive dependencies to queue
            if (versionInfo.dependencies() != null && !versionInfo.dependencies().isEmpty()) {
                for (Map.Entry<String, String> dep : versionInfo.dependencies().entrySet()) {
                    String depName = dep.getKey();
                    String depConstraint = dep.getValue();
                    queue.add(new DependencySpec(depName, depConstraint));
                }
            }
        }

        // Create lock file
        LockFile lockFile = new LockFile();
        for (ResolvedPackage pkg : resolved.values()) {
            lockFile.addPackage(pkg);
        }

        logger.info("Resolved {} packages", resolved.size());
        return lockFile;
    }

    /**
     * Checks if a package is already resolved.
     */
    private boolean isAlreadyResolved(Map<String, ResolvedPackage> resolved, String packageName) {
        return resolved.values().stream()
            .anyMatch(pkg -> pkg.name().equals(packageName));
    }

    /**
     * Finds a resolved package by name.
     */
    private ResolvedPackage findResolvedPackage(Map<String, ResolvedPackage> resolved, String packageName) {
        return resolved.values().stream()
            .filter(pkg -> pkg.name().equals(packageName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Fetches every available version of a package, keyed by version string. This combines the registry's package
     * index with packages published locally via {@code qpm publish-local}, with local copies taking precedence when
     * the same version exists in both. If local versions exist, a registry failure is tolerated so local-only
     * packages can still be resolved.
     */
    private Map<String, VersionInfo> fetchAvailableVersions(String packageName) throws ResolutionException {
        var packageNameScope = RegistryClient.parsePackageName(packageName);
        var localVersions = localPackageIndex.getVersions(packageNameScope.scope(), packageNameScope.name());

        Map<String, VersionInfo> versionMap = new LinkedHashMap<>();

        try {
            PackageVersionList versionList = registryClient.getPackageVersions(packageNameScope.scope(), packageNameScope.name());
            versionList.versions().forEach(versionInfo -> versionMap.put(versionInfo.version(), versionInfo));
        } catch (RegistryException e) {
            if (localVersions.isEmpty()) {
                throw new ResolutionException("Failed to fetch package index for " + packageName, e);
            }

            logger.warn("Failed to fetch registry index for {}, using local versions only: {}", packageName, e.getMessage());
        }

        for (VersionInfo localVersion : localVersions) {
            versionMap.put(localVersion.version(), localVersion);
            localVersionStrings.add(packageName + "@" + localVersion.version());
        }

        if (versionMap.isEmpty()) {
            throw new ResolutionException("No versions found for package: " + packageName);
        }

        return versionMap;
    }
}
