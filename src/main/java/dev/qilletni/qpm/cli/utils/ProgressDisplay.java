package dev.qilletni.qpm.cli.utils;

/**
 * Utility class for displaying progress information to the user with color support.
 */
public class ProgressDisplay {

    /**
     * Displays a simple progress message.
     *
     * @param message the message to display
     */
    public static void info(String message) {
        System.out.println(message);
    }

    /**
     * Displays an info message with an action highlighted in cyan.
     *
     * @param action the action being performed (e.g., "Downloading", "Installing")
     * @param target the target of the action
     */
    public static void infoAction(String action, String target) {
        System.out.println(ColorSupport.cyan(action) + " " + target);
    }

    /**
     * Displays an error message in red.
     *
     * @param message the error message
     */
    public static void error(String message) {
        System.err.println(ColorSupport.red("Error:") + " " + message);
    }

    /**
     * Displays a success message in green with a checkmark.
     *
     * @param message the success message
     */
    public static void success(String message) {
        System.out.println(ColorSupport.green("✓") + " " + message);
    }

    /**
     * Displays a warning message in yellow.
     *
     * @param message the warning message
     */
    public static void warn(String message) {
        System.out.println(ColorSupport.yellow("⚠") + " " + message);
    }

    /**
     * Formats a package name with bold styling.
     *
     * @param packageName the package name
     * @return formatted package name
     */
    public static String formatPackageName(String packageName) {
        return ColorSupport.bold(packageName);
    }

    /**
     * Formats a version string with cyan color.
     *
     * @param version the version string
     * @return formatted version
     */
    public static String formatVersion(String version) {
        return ColorSupport.cyan(version);
    }

    /**
     * Formats technical data (like hashes) with dim styling.
     *
     * @param data the technical data
     * @return formatted data
     */
    public static String formatTechnical(String data) {
        return ColorSupport.dim(data);
    }

    /**
     * Displays a download progress message with colored action.
     *
     * @param packageName the package being downloaded
     * @param current     current bytes downloaded
     * @param total       total bytes to download
     */
    public static void downloadProgress(String packageName, long current, long total) {
        int percent = (int) ((current * 100) / total);
        String action = ColorSupport.cyan("Downloading");
        System.out.printf("\r%s %s... %d%%", action, packageName, percent);
        if (current >= total) {
            System.out.println(); // New line when complete
        }
    }

    /**
     * Displays an upload progress message with colored action.
     *
     * @param current current bytes uploaded
     * @param total   total bytes to upload
     */
    public static void uploadProgress(long current, long total) {
        int percent = (int) ((current * 100) / total);
        String action = ColorSupport.cyan("Uploading package");
        System.out.printf("\r%s... %d%%", action, percent);
        if (current >= total) {
            System.out.println(); // New line when complete
        }
    }

    /**
     * Formats bytes to human-readable format.
     *
     * @param bytes the number of bytes
     * @return formatted string (e.g., "1.5 MB")
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
