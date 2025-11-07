package vn.ptit.p2p.common;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * General utility functions
 */
public class Utils {
    
    /**
     * Generate a unique peer ID
     */
    public static String generatePeerId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Get the local host address
     */
    public static String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
    
    /**
     * Format bytes to human-readable string
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Format duration in milliseconds to human-readable string
     */
    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Create directory if it doesn't exist
     */
    public static void ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
    
    /**
     * Calculate transfer rate in bytes per second
     */
    public static double calculateTransferRate(long bytes, long milliseconds) {
        if (milliseconds == 0) {
            return 0;
        }
        return (bytes * 1000.0) / milliseconds;
    }
    
    /**
     * Format transfer rate to human-readable string
     */
    public static String formatTransferRate(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format("%.2f B/s", bytesPerSecond);
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.2f KB/s", bytesPerSecond / 1024);
        } else {
            return String.format("%.2f MB/s", bytesPerSecond / (1024 * 1024));
        }
    }
    
    /**
     * Validate file hash format
     */
    public static boolean isValidHash(String hash) {
        return hash != null && hash.matches("^[a-f0-9]{64}$");
    }
}

