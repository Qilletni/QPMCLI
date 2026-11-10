package dev.qilletni.qpm.cli.manifest;

import dev.qilletni.qpm.cli.exceptions.RegistryException;
import dev.qilletni.qpm.cli.exceptions.ResolutionException;
import dev.qilletni.qpm.cli.models.DependencySpec;
import dev.qilletni.qpm.cli.models.PackageVersionList;
import dev.qilletni.qpm.cli.models.VersionInfo;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DependencyResolverTest {

    @TempDir
    Path localDir;

    /**
     * A registry stub that serves a fixed set of versions per package, and 404s for anything else.
     */
    private static RegistryClient registryWith(Map<String, List<VersionInfo>> packages) {
        return new RegistryClient("http://localhost:0") {
            @Override
            public PackageVersionList getPackageVersions(String scope, String name) throws RegistryException {
                var versions = packages.get(scope + "/" + name);
                if (versions == null) {
                    throw new RegistryException("Not found", 404);
                }
                return new PackageVersionList(scope + "/" + name, versions, versions.getLast());
            }
        };
    }

    private static VersionInfo registryVersion(String version, Map<String, String> deps) {
        return new VersionInfo(version, "sha256-registry" + version, 1, Instant.EPOCH, deps);
    }

    private void publishLocal(String scope, String name, String version, String dependenciesJson) throws IOException {
        var file = localDir.resolve(scope).resolve(name).resolve(name + "-" + version + ".qll");
        Files.createDirectories(file.getParent());

        var info = """
                {"scope":"%s","name":"%s","version":"%s","dependencies":%s}""".formatted(scope, name, version, dependenciesJson);

        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("qll.info"));
            zip.write(info.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private static Manifest manifest(DependencySpec... deps) {
        return new Manifest("test/app", "1.0.0", List.of(deps));
    }

    @Test
    void localVersionSatisfiesConstraintTheRegistryCannot() throws Exception {
        var registry = registryWith(Map.of("qilletni/std", List.of(registryVersion("1.0.0", Map.of()))));
        publishLocal("qilletni", "std", "1.0.2", "[]");

        var lockFile = new DependencyResolver(registry, localDir)
                .resolve(manifest(new DependencySpec("qilletni/std", "^1.0.2")));

        var pkg = lockFile.getPackages().values().iterator().next();
        assertEquals("qilletni/std", pkg.name());
        assertEquals("1.0.2", pkg.version());
    }

    @Test
    void picksHighestAcrossRegistryAndLocal() throws Exception {
        var registry = registryWith(Map.of("qilletni/std", List.of(registryVersion("1.0.0", Map.of()), registryVersion("1.0.5", Map.of()))));
        publishLocal("qilletni", "std", "1.0.2", "[]");

        var lockFile = new DependencyResolver(registry, localDir)
                .resolve(manifest(new DependencySpec("qilletni/std", "^1.0.0")));

        assertEquals("1.0.5", lockFile.getPackages().values().iterator().next().version());
    }

    @Test
    void localOnlyPackageResolvesWithTransitiveDependencies() throws Exception {
        var registry = registryWith(Map.of("qilletni/std", List.of(registryVersion("1.0.0", Map.of()))));
        publishLocal("qilletni", "local", "2.0.0", "[{\"scope\":\"qilletni\",\"name\":\"std\",\"version\":\"^1.0.0\"}]");

        var lockFile = new DependencyResolver(registry, localDir)
                .resolve(manifest(new DependencySpec("qilletni/local", "^2.0.0")));

        var resolved = lockFile.getPackages().values().stream()
                .map(pkg -> pkg.name() + "@" + pkg.version())
                .toList();
        assertTrue(resolved.contains("qilletni/local@2.0.0"), resolved.toString());
        assertTrue(resolved.contains("qilletni/std@1.0.0"), resolved.toString());
    }

    @Test
    void missingEverywhereStillFails() {
        var registry = registryWith(Map.of());

        assertThrows(ResolutionException.class, () -> new DependencyResolver(registry, localDir)
                .resolve(manifest(new DependencySpec("qilletni/nope", "^1.0.0"))));
    }
}