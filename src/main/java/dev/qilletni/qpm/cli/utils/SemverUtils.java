package dev.qilletni.qpm.cli.utils;

import dev.qilletni.qpm.cli.models.Version;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for semantic versioning operations.
 * Supports version parsing, constraint matching, and version comparison.
 */
public class SemverUtils {

    /**
     * Checks if a version satisfies a constraint.
     * Supported constraints:
     * - Exact: "1.0.0" - must match exactly
     * - Caret: "^1.0.0" - >= 1.0.0 and < 2.0.0 (compatible with X)
     * - Tilde: "~1.0.0" - >= 1.0.0 and < 1.1.0 (approximately equivalent to X.Y)
     *
     * @param version    the version to check
     * @param constraint the constraint string
     * @return true if the version satisfies the constraint
     */
    public static boolean satisfies(Version version, String constraint) {
        if (constraint == null || constraint.isEmpty()) {
            throw new IllegalArgumentException("Constraint cannot be null or empty");
        }

        constraint = constraint.trim();

        // Handle caret constraint (^)
        if (constraint.startsWith("^")) {
            String versionStr = constraint.substring(1);
            Version minVersion = Version.parse(versionStr);
            return satisfiesCaret(version, minVersion);
        }

        // Handle tilde constraint (~)
        if (constraint.startsWith("~")) {
            String versionStr = constraint.substring(1);
            Version minVersion = Version.parse(versionStr);
            return satisfiesTilde(version, minVersion);
        }

        // Exact match
        Version exactVersion = Version.parse(constraint);
        return version.equals(exactVersion);
    }

    /**
     * Checks if a version satisfies a caret constraint.
     * Caret allows changes that do not modify the left-most non-zero digit.
     * Examples:
     * - ^1.2.3 means >=1.2.3 <2.0.0
     * - ^0.2.3 means >=0.2.3 <0.3.0
     * - ^0.0.3 means >=0.0.3 <0.0.4
     *
     * @param version    the version to check
     * @param minVersion the minimum version from the constraint
     * @return true if satisfied
     */
    private static boolean satisfiesCaret(Version version, Version minVersion) {
        // Version must be >= minVersion
        if (version.compareTo(minVersion) < 0) {
            return false;
        }

        // Determine the upper bound based on the left-most non-zero digit
        if (minVersion.getMajor() > 0) {
            // If major > 0: allow any minor/patch in the same major version
            return version.getMajor() == minVersion.getMajor();
        } else if (minVersion.getMinor() > 0) {
            // If major = 0 and minor > 0: allow any patch in the same minor version
            return version.getMajor() == 0 && version.getMinor() == minVersion.getMinor();
        } else {
            // If major = 0 and minor = 0: only allow exact patch version
            return version.getMajor() == 0 && version.getMinor() == 0 &&
                   version.getPatch() == minVersion.getPatch();
        }
    }

    /**
     * Checks if a version satisfies a tilde constraint.
     * Tilde allows patch-level changes.
     * Examples:
     * - ~1.2.3 means >=1.2.3 <1.3.0
     * - ~1.2 means >=1.2.0 <1.3.0
     *
     * @param version    the version to check
     * @param minVersion the minimum version from the constraint
     * @return true if satisfied
     */
    private static boolean satisfiesTilde(Version version, Version minVersion) {
        // Version must be >= minVersion
        if (version.compareTo(minVersion) < 0) {
            return false;
        }

        // Must match major and minor exactly
        return version.getMajor() == minVersion.getMajor() &&
               version.getMinor() == minVersion.getMinor();
    }

    /**
     * Finds the maximum version that satisfies a constraint from a list of versions.
     *
     * @param versions   the list of available version strings
     * @param constraint the constraint string
     * @return the maximum satisfying version, or null if none found
     */
    public static Version findMaxSatisfying(List<String> versions, String constraint) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.stream()
            .map(Version::parse)
            .filter(v -> satisfies(v, constraint))
            .max(Version::compareTo)
            .orElse(null);
    }

    /**
     * Parses a list of version strings into Version objects.
     *
     * @param versionStrings the list of version strings
     * @return the list of Version objects
     */
    public static List<Version> parseVersions(List<String> versionStrings) {
        return versionStrings.stream()
            .map(Version::parse)
            .collect(Collectors.toList());
    }

    /**
     * Finds the latest (maximum) version from a list of version strings.
     *
     * @param versions the list of version strings
     * @return the latest version, or null if the list is empty
     */
    public static Version findLatest(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.stream()
            .map(Version::parse)
            .max(Version::compareTo)
            .orElse(null);
    }

    /**
     * Validates a version constraint format.
     *
     * @param constraint the constraint string
     * @return true if valid
     */
    public static boolean isValidConstraint(String constraint) {
        if (constraint == null || constraint.isEmpty()) {
            return false;
        }

        constraint = constraint.trim();

        try {
            // Try to parse as is or without prefix
            if (constraint.startsWith("^") || constraint.startsWith("~")) {
                Version.parse(constraint.substring(1));
            } else {
                Version.parse(constraint);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
