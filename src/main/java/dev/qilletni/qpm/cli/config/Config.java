package dev.qilletni.qpm.cli.config;

/**
 * Represents the CLI configuration stored in ~/.qilletni/config.json
 */
public class Config {
    private String registryUrl;
    private GithubConfig github;

    public Config() {
        this.registryUrl = "https://qpm.qilletni.dev"; // Default registry URL
        this.github = new GithubConfig();
    }

    public Config(String registryUrl, GithubConfig github) {
        this.registryUrl = registryUrl;
        this.github = github;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public GithubConfig getGithub() {
        return github;
    }

    public void setGithub(GithubConfig github) {
        this.github = github;
    }

    /**
     * GitHub-specific configuration.
     */
    public static class GithubConfig {
        private String token;

        public GithubConfig() {
        }

        public GithubConfig(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public boolean hasToken() {
            return token != null && !token.isEmpty();
        }
    }
}
