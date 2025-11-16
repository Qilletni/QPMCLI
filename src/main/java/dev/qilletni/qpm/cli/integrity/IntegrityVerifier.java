package dev.qilletni.qpm.cli.integrity;

import dev.qilletni.qpm.cli.exceptions.IntegrityException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for computing and verifying package integrity hashes.
 * Uses SHA-256 algorithm with base64 encoding.
 */
public class IntegrityVerifier {

    /**
     * Computes the SHA-256 integrity hash of a file.
     *
     * @param filePath the path to the file
     * @return the integrity string in format "sha256-{base64hash}"
     * @throws IOException if there's an error reading the file
     */
    public static String computeIntegrity(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Read file and compute hash
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            // Get hash bytes and encode to base64
            byte[] hashBytes = digest.digest();
            String base64Hash = Base64.getEncoder().encodeToString(hashBytes);

            return "sha256-" + base64Hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Computes the SHA-256 integrity hash of a byte array.
     *
     * @param data the byte array
     * @return the integrity string in format "sha256-{base64hash}"
     */
    public static String computeIntegrity(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            String base64Hash = Base64.getEncoder().encodeToString(hashBytes);
            return "sha256-" + base64Hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies that a file's integrity matches the expected hash.
     *
     * @param filePath         the path to the file
     * @param expectedIntegrity the expected integrity string
     * @throws IntegrityException if the integrity check fails
     * @throws IOException        if there's an error reading the file
     */
    public static void verifyIntegrity(Path filePath, String expectedIntegrity)
        throws IntegrityException, IOException {
        String actualIntegrity = computeIntegrity(filePath);

        if (!actualIntegrity.equals(expectedIntegrity)) {
            throw new IntegrityException(
                "Integrity verification failed for " + filePath.getFileName(),
                expectedIntegrity,
                actualIntegrity
            );
        }
    }

    /**
     * Verifies that a byte array's integrity matches the expected hash.
     *
     * @param data              the byte array
     * @param expectedIntegrity the expected integrity string
     * @param description       description of the data (for error messages)
     * @throws IntegrityException if the integrity check fails
     */
    public static void verifyIntegrity(byte[] data, String expectedIntegrity, String description)
        throws IntegrityException {
        String actualIntegrity = computeIntegrity(data);

        if (!actualIntegrity.equals(expectedIntegrity)) {
            throw new IntegrityException(
                "Integrity verification failed for " + description,
                expectedIntegrity,
                actualIntegrity
            );
        }
    }

    /**
     * Checks if an integrity string is valid (format: sha256-{base64}).
     *
     * @param integrity the integrity string to validate
     * @return true if valid format
     */
    public static boolean isValidIntegrity(String integrity) {
        if (integrity == null || integrity.isEmpty()) {
            return false;
        }

        if (!integrity.startsWith("sha256-")) {
            return false;
        }

        String base64Part = integrity.substring(7);
        try {
            Base64.getDecoder().decode(base64Part);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
