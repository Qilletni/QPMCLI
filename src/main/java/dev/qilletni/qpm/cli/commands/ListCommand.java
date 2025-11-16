package dev.qilletni.qpm.cli.commands;

import dev.qilletni.qpm.cli.models.PackageListResponse;
import dev.qilletni.qpm.cli.registry.RegistryClient;
import dev.qilletni.qpm.cli.utils.ColorSupport;
import dev.qilletni.qpm.cli.utils.ProgressDisplay;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * List command - lists all packages in the registry.
 */
@Command(
    name = "list",
    description = "List all packages in the registry"
)
public class ListCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message")
    private boolean helpRequested;

    @Option(names = {"--scope"}, description = "Filter packages by scope (e.g., alice)")
    private String scopeFilter;

    private final RegistryClient registryClient;

    public ListCommand() {
        this.registryClient = new RegistryClient();
    }

    @Override
    public Integer call() {
        try {
            // Fetch all packages from registry
            ProgressDisplay.info("Fetching packages from registry...");
            PackageListResponse response = registryClient.listAllPackages();

            List<PackageListResponse.PackageInfo> packages = response.packages();

            // Apply scope filter if provided
            if (scopeFilter != null && !scopeFilter.isEmpty()) {
                String filterPrefix = "@" + scopeFilter + "/";
                packages = packages.stream()
                    .filter(pkg -> pkg.name().startsWith(filterPrefix))
                    .collect(Collectors.toList());

                if (packages.isEmpty()) {
                    ProgressDisplay.info("No packages found for scope: @" + scopeFilter);
                    return 0;
                }
            }

            // Display results
            if (packages.isEmpty()) {
                ProgressDisplay.info("No packages found in registry.");
                return 0;
            }

            System.out.println();
            displayPackagesTable(packages);
            System.out.println();

            if (scopeFilter != null && !scopeFilter.isEmpty()) {
                ProgressDisplay.info(String.format("Found %d package(s) for scope @%s", packages.size(), scopeFilter));
            } else {
                ProgressDisplay.info(String.format("Total: %d package(s)", response.total()));
            }

            return 0;

        } catch (Exception e) {
            ProgressDisplay.error("Failed to list packages: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Displays packages in a formatted table with color formatting.
     *
     * @param packages the list of packages to display
     */
    private void displayPackagesTable(List<PackageListResponse.PackageInfo> packages) {
        // Calculate column widths
        int nameWidth = Math.max(20, packages.stream()
            .mapToInt(p -> p.name().length())
            .max()
            .orElse(20));
        int versionWidth = 15;
        int countWidth = 10;

        // Print header with bold/bright styling
        String separator = "-".repeat(nameWidth + versionWidth + countWidth + 4);

        System.out.printf("%s%s  %s%s  %s%n",
            ColorSupport.brightWhite("PACKAGE"),
            " ".repeat(nameWidth - "PACKAGE".length()),
            ColorSupport.brightWhite("LATEST VERSION"),
            " ".repeat(versionWidth - "LATEST VERSION".length()),
            ColorSupport.brightWhite("VERSIONS"));
        System.out.println(separator);

        // Print rows with colored package names and versions
        for (PackageListResponse.PackageInfo pkg : packages) {
            // Format each field with appropriate width, accounting for ANSI codes
            String formattedName = ColorSupport.bold(pkg.name());
            String formattedVersion = ColorSupport.cyan(pkg.latest());

            // Calculate padding needed (ANSI codes don't count towards width)
            int namePadding = nameWidth - pkg.name().length();
            int versionPadding = versionWidth - pkg.latest().length();

            System.out.printf("%s%s  %s%s  %d%n",
                formattedName,
                " ".repeat(Math.max(0, namePadding)),
                formattedVersion,
                " ".repeat(Math.max(0, versionPadding)),
                pkg.versionCount());
        }
    }
}
