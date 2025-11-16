package dev.qilletni.qpm.cli.models;

/**
 * Represents a package dependency with name and version constraint.
 */
public record Dependency(String name, String versionConstraint) {
    public Dependency {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Dependency name cannot be null or empty");
        }
        if (versionConstraint == null || versionConstraint.isEmpty()) {
            throw new IllegalArgumentException("Version constraint cannot be null or empty");
        }
    }
}
