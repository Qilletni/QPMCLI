package dev.qilletni.qpm.cli.manifest;

import dev.qilletni.pkgutil.manifest.LockFile;
import dev.qilletni.pkgutil.manifest.models.ResolvedPackage;
import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.exceptions.ResolutionException;
import dev.qilletni.qpm.cli.models.*;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.SemverUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Resolves package dependencies using a breadth-first algorithm.
 * Implements simple one-version-per-package resolution with conflict detection.
 */
public class DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);

    private final RegistryClient registryClient;

    public DependencyResolver(RegistryClient registryClient) {
        this.registryClient = registryClient;
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

                if (!SemverUtils.satisfies(existingVersion, constraint)) {
                    throw new ResolutionException(String.format(
                        "Conflict: %s requires version %s but %s is already resolved",
                        packageName, constraint, existingVersion
                    ));
                }

                logger.debug("Package {} already resolved with compatible version {}", packageName, existingVersion);
                continue;
            }

            // Fetch package index with all version details
            PackageVersionList versionList = fetchPackageIndex(packageName);
            if (versionList.versions().isEmpty()) {
                throw new ResolutionException("No versions found for package: " + packageName);
            }

            // Get available version strings
            List<String> availableVersions = versionList.getVersionStrings();

            // Find best matching version
            Version bestVersion = SemverUtils.findMaxSatisfying(availableVersions, constraint);
            if (bestVersion == null) {
                throw new ResolutionException(String.format(
                    "No version of %s satisfies constraint: %s (available: %s)",
                    packageName, constraint, String.join(", ", availableVersions)
                ));
            }

            logger.info("Resolved {} to version {}", packageName, bestVersion);

            // Get the version info from the index (already fetched)
            VersionInfo versionInfo = versionList.findVersion(bestVersion.toString());
            if (versionInfo == null) {
                throw new ResolutionException("Version info not found for " + packageName + "@" + bestVersion);
            }

            // Create resolved package using data from the index
            var packageNameScope = RegistryClient.parsePackageName(packageName);
            String resolvedUrl = String.format("%s/%s/%s", packageNameScope.scope(), packageNameScope.name(), bestVersion);

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
     * Fetches the package index with all version details from the registry.
     * This is the optimized approach - single request contains all version info including dependencies.
     */
    private PackageVersionList fetchPackageIndex(String packageName) throws ResolutionException {
        try {
            var packageNameScope = RegistryClient.parsePackageName(packageName);

            return registryClient.getPackageVersions(packageNameScope.scope(), packageNameScope.name());
        } catch (RegistryException e) {
            throw new ResolutionException("Failed to fetch package index for " + packageName, e);
        }
    }
}
