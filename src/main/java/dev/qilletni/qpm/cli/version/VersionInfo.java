package dev.qilletni.qpm.cli.version;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The embedded build/version metadata baked into the QPM archive at build time
 * (see the {@code generateVersionProperties} Gradle task), exposing this
 * repository's own version alongside the exact upstream {@code qilletni-api}
 * and {@code qilletni-pkgutil} versions it was built against, plus the source
 * commit. Backs both {@code qpm --version} and {@code component-manifest.json}.
 */
public record VersionInfo(String qpmVersion, String apiVersion, String pkgutilVersion, String commit) {

    private static final String UNKNOWN = "unknown";
    private static final String RESOURCE_NAME = "/version.properties";

    public static VersionInfo fromProperties(Properties properties) {
        return new VersionInfo(
                properties.getProperty("qpm.version", UNKNOWN),
                properties.getProperty("qilletni.api.version", UNKNOWN),
                properties.getProperty("qilletni.pkgutil.version", UNKNOWN),
                properties.getProperty("qpm.commit", UNKNOWN)
        );
    }

    /**
     * Loads {@link VersionInfo} from the {@code version.properties} resource generated onto the
     * classpath at build time. Falls back to {@code "unknown"} for every field if the resource is
     * absent (e.g. running directly from an IDE without having run the Gradle build).
     */
    public static VersionInfo load() {
        return load(VersionInfo.class);
    }

    static VersionInfo load(Class<?> resourceOwner) {
        var properties = new Properties();

        try (InputStream in = resourceOwner.getResourceAsStream(RESOURCE_NAME)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + RESOURCE_NAME, e);
        }

        return fromProperties(properties);
    }
}
