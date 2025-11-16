package dev.qilletni.qpm.cli.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the CLI configuration file (~/.qilletni/config.json).
 * Handles reading, writing, and auto-creating the config file.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".qilletni");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Gets the configuration directory path.
     */
    public static Path getConfigDir() {
        return CONFIG_DIR;
    }

    /**
     * Gets the configuration file path.
     */
    public static Path getConfigFile() {
        return CONFIG_FILE;
    }

    /**
     * Loads the configuration from the config file.
     * If the file doesn't exist, creates it with default values.
     *
     * @return the loaded Config object
     * @throws IOException if there's an error reading or creating the file
     */
    public static Config loadConfig() throws IOException {
        // Ensure config directory exists
        if (!Files.exists(CONFIG_DIR)) {
            logger.debug("Creating config directory: {}", CONFIG_DIR);
            Files.createDirectories(CONFIG_DIR);
        }

        // If config file doesn't exist, create it with defaults
        if (!Files.exists(CONFIG_FILE)) {
            logger.info("Config file not found, creating default configuration at: {}", CONFIG_FILE);
            Config defaultConfig = new Config();
            saveConfig(defaultConfig);
            return defaultConfig;
        }

        // Read and parse config file
        try {
            String json = Files.readString(CONFIG_FILE);
            Config config = gson.fromJson(json, Config.class);

            // Ensure config has all required fields
            if (config == null) {
                logger.warn("Config file is empty or invalid, using defaults");
                config = new Config();
            }
            if (config.getGithub() == null) {
                config.setGithub(new Config.GithubConfig());
            }

            return config;
        } catch (Exception e) {
            logger.error("Failed to parse config file, using defaults", e);
            return new Config();
        }
    }

    /**
     * Saves the configuration to the config file.
     *
     * @param config the Config object to save
     * @throws IOException if there's an error writing the file
     */
    public static void saveConfig(Config config) throws IOException {
        // Ensure config directory exists
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        // Write config to file
        String json = gson.toJson(config);
        Files.writeString(CONFIG_FILE, json);
        logger.debug("Config saved to: {}", CONFIG_FILE);
    }

    /**
     * Updates the GitHub token in the config.
     *
     * @param token the GitHub token
     * @throws IOException if there's an error saving the config
     */
    public static void updateToken(String token) throws IOException {
        Config config = loadConfig();
        config.getGithub().setToken(token);
        saveConfig(config);
    }

    /**
     * Clears the GitHub token from the config.
     *
     * @throws IOException if there's an error saving the config
     */
    public static void clearToken() throws IOException {
        Config config = loadConfig();
        config.getGithub().setToken(null);
        saveConfig(config);
    }

    /**
     * Checks if a GitHub token is configured.
     *
     * @return true if a token exists, false otherwise
     */
    public static boolean hasToken() {
        try {
            Config config = loadConfig();
            return config.getGithub().hasToken();
        } catch (IOException e) {
            logger.error("Failed to check for token", e);
            return false;
        }
    }

    /**
     * Gets the GitHub token from the config.
     *
     * @return the token, or null if not configured
     */
    public static String getToken() {
        try {
            Config config = loadConfig();
            return config.getGithub().getToken();
        } catch (IOException e) {
            logger.error("Failed to get token", e);
            return null;
        }
    }

    /**
     * Gets the registry URL from the config.
     *
     * @return the registry URL
     */
    public static String getRegistryUrl() {
        try {
            Config config = loadConfig();
            var url = config.getRegistryUrl();
            if (url.endsWith("/")) {
                url =  url.substring(0, url.length() - 1);
            }

            return url;
        } catch (IOException e) {
            logger.error("Failed to get registry URL, using default", e);
            return new Config().getRegistryUrl();
        }
    }

    /**
     * Gets the packages directory path (~/.qilletni/packages).
     */
    public static Path getPackagesDir() {
        return CONFIG_DIR.resolve("packages");
    }

    /**
     * Ensures the packages directory exists.
     */
    public static void ensurePackagesDir() throws IOException {
        Path packagesDir = getPackagesDir();
        if (!Files.exists(packagesDir)) {
            Files.createDirectories(packagesDir);
        }
    }
}
