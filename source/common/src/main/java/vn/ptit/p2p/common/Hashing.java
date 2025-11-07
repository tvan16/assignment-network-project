package vn.ptit.p2p.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing utilities for file integrity verification
 */
public class Hashing {
    private static final String ALGORITHM = "SHA-256";
    
    /**
     * Calculate SHA-256 hash of a file
     */
    public static String hashFile(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Calculate SHA-256 hash of a byte array
     */
    public static String hashBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(data);
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Calculate SHA-256 hash of a string
     */
    public static String hashString(String data) {
        return hashBytes(data.getBytes());
    }
    
    /**
     * Convert byte array to hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Verify if data matches the expected hash
     */
    public static boolean verify(byte[] data, String expectedHash) {
        String actualHash = hashBytes(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }
}

