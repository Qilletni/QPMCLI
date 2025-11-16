package dev.qilletni.qpm.cli.models;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a semantic version (MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]).
 */
public class Version implements Comparable<Version> {
    private static final Pattern VERSION_PATTERN =
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9.-]+))?(?:\\+([a-zA-Z0-9.-]+))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String prerelease;
    private final String buildMetadata;

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, null, null);
    }

    public Version(int major, int minor, int patch, String prerelease, String buildMetadata) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease;
        this.buildMetadata = buildMetadata;
    }

    /**
     * Parses a version string into a Version object.
     *
     * @param versionString the version string (e.g., "1.0.0", "1.2.3-SNAPSHOT")
     * @return the parsed Version
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static Version parse(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + versionString);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String prerelease = matcher.group(4);
        String buildMetadata = matcher.group(5);

        return new Version(major, minor, patch, prerelease, buildMetadata);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPrerelease() {
        return prerelease;
    }

    public String getBuildMetadata() {
        return buildMetadata;
    }

    public boolean isPrerelease() {
        return prerelease != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (prerelease != null) {
            sb.append('-').append(prerelease);
        }
        if (buildMetadata != null) {
            sb.append('+').append(buildMetadata);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Version other) {
        // Compare major.minor.patch
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        // Prerelease versions have lower precedence
        if (this.prerelease == null && other.prerelease == null) return 0;
        if (this.prerelease == null) return 1;  // this > other
        if (other.prerelease == null) return -1; // this < other

        // Compare prerelease identifiers lexically
        return this.prerelease.compareTo(other.prerelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
               minor == version.minor &&
               patch == version.patch &&
               Objects.equals(prerelease, version.prerelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, prerelease);
    }
}
