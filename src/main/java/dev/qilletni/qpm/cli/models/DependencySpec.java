package dev.qilletni.qpm.cli.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a dependency specification.
 * In qilletni_info.yml, dependencies are specified as a map:
 *   dependencies:
 *     alice/postgres: ^1.0.0
 *     bob/utils: 1.0.0
 */
public record DependencySpec(String packageName, String versionConstraint) {
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^([^:]+):(@[^/]+/[^@]+)$");

    public DependencySpec {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (versionConstraint == null || versionConstraint.isEmpty()) {
            throw new IllegalArgumentException("Version constraint cannot be null or empty");
        }
        if (!packageName.matches("^[a-zA-Z0-9-]+/[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid package name format: " + packageName);
        }
    }

    /**
     * Parses a dependency specification string.
     *
     * @param depString the dependency string (e.g., "^1.0.0:@alice/postgres")
     * @return the parsed DependencySpec
     * @throws IllegalArgumentException if the format is invalid
     */
    public static DependencySpec parse(String depString) {
        if (depString == null || depString.isEmpty()) {
            throw new IllegalArgumentException("Dependency string cannot be null or empty");
        }

        Matcher matcher = DEPENDENCY_PATTERN.matcher(depString.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid dependency format: " + depString);
        }

        String versionConstraint = matcher.group(1).trim();
        String packageName = matcher.group(2).trim();

        return new DependencySpec(packageName, versionConstraint);
    }

    @Override
    public String toString() {
        return versionConstraint + ":" + packageName;
    }
}
