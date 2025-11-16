package dev.qilletni.qpm.cli.manifest;

import dev.qilletni.qpm.cli.models.DependencySpec;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents and parses a manifest.dsl file.
 * Format:
 * name: @username/package
 * version: 1.0.0
 * dependencies:
 * name1/pkg: 1.0.0
 * name2/pkg1: ^1.0.0
 */
public record Manifest(String name, String version, List<DependencySpec> dependencies) {
    public Manifest(String name, String version, List<DependencySpec> dependencies) {
        this.name = name;
        this.version = version;
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    /**
     * Parses a manifest.dsl file from the given path.
     *
     * @param manifestPath the path to the manifest.dsl file
     * @return the parsed Manifest
     * @throws IOException if there's an error reading the file
     */
    public static Manifest parse(Path manifestPath) throws IOException {
        if (!Files.exists(manifestPath)) {
            throw new IOException("Manifest file not found: " + manifestPath);
        }

        String content = Files.readString(manifestPath);
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(content);

        if (data == null) {
            throw new IOException("Manifest file is empty or invalid");
        }

        var name = (String) data.get("name");
        var version = (String) data.get("version");

        if (name == null || name.isEmpty()) {
            throw new IOException("Manifest must contain a 'name' field");
        }
        if (version == null || version.isEmpty()) {
            throw new IOException("Manifest must contain a 'version' field");
        }

        // Parse dependencies (new format: map of package name to version constraint)
        List<DependencySpec> dependencies = new ArrayList<>();
        Object depsObj = data.get("dependencies");

        if (depsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> depsMap = (Map<String, String>) depsObj;
            for (Map.Entry<String, String> entry : depsMap.entrySet()) {
                try {
                    var packageName = entry.getKey();
                    var versionConstraint = entry.getValue();

                    DependencySpec dep = new DependencySpec(packageName, versionConstraint);
                    dependencies.add(dep);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid dependency: " + entry.getKey() + ": " + entry.getValue(), e);
                }
            }
        }

        return new Manifest(name, version, dependencies);
    }

    /**
     * Writes the manifest to a file.
     *
     * @param manifestPath the path where to write the manifest
     * @throws IOException if there's an error writing the file
     */
    public void write(Path manifestPath) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("name: ").append(name).append("\n");
        content.append("version: ").append(version).append("\n");

        if (!dependencies.isEmpty()) {
            content.append("dependencies:\n");
            for (DependencySpec dep : dependencies) {
                // Write in new format: package_name: version (without @ prefix)
                String packageName = dep.packageName();
                if (packageName.startsWith("@")) {
                    packageName = packageName.substring(1);
                }
                content.append("  ").append(packageName).append(": ").append(dep.versionConstraint()).append("\n");
            }
        }

        Files.writeString(manifestPath, content.toString());
    }
}
