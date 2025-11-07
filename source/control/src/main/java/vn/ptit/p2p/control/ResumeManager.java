package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages resumption of interrupted transfers
 */
public class ResumeManager {
    private static final Logger logger = LoggerFactory.getLogger(ResumeManager.class);
    private static final String RESUME_FILE_EXT = ".p2presume";
    
    private final String resumeDir;
    
    public ResumeManager(String resumeDir) {
        this.resumeDir = resumeDir;
        try {
            Files.createDirectories(Paths.get(resumeDir));
        } catch (IOException e) {
            logger.error("Failed to create resume directory", e);
        }
    }
    
    /**
     * Save download state for later resumption
     */
    public void saveDownloadState(String fileHash, BitSet downloadedPieces, String outputPath) {
        Path resumePath = getResumePath(fileHash);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(resumePath.toFile()))) {
            Map<String, Object> state = new HashMap<>();
            state.put("fileHash", fileHash);
            state.put("downloadedPieces", downloadedPieces);
            state.put("outputPath", outputPath);
            state.put("timestamp", System.currentTimeMillis());
            
            oos.writeObject(state);
            logger.debug("Saved resume state for file {}", fileHash);
        } catch (IOException e) {
            logger.error("Failed to save resume state", e);
        }
    }
    
    /**
     * Load download state for resumption
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadDownloadState(String fileHash) {
        Path resumePath = getResumePath(fileHash);
        
        if (!Files.exists(resumePath)) {
            return null;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(resumePath.toFile()))) {
            Map<String, Object> state = (Map<String, Object>) ois.readObject();
            logger.info("Loaded resume state for file {}", fileHash);
            return state;
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to load resume state", e);
            return null;
        }
    }
    
    /**
     * Delete resume state after successful download
     */
    public void deleteResumeState(String fileHash) {
        Path resumePath = getResumePath(fileHash);
        
        try {
            Files.deleteIfExists(resumePath);
            logger.debug("Deleted resume state for file {}", fileHash);
        } catch (IOException e) {
            logger.error("Failed to delete resume state", e);
        }
    }
    
    /**
     * Check if resume state exists
     */
    public boolean hasResumeState(String fileHash) {
        return Files.exists(getResumePath(fileHash));
    }
    
    /**
     * Get all resumable downloads
     */
    public List<String> getResumableDownloads() {
        List<String> resumable = new ArrayList<>();
        File dir = new File(resumeDir);
        
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(RESUME_FILE_EXT));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String fileHash = fileName.substring(0, fileName.length() - RESUME_FILE_EXT.length());
                    resumable.add(fileHash);
                }
            }
        }
        
        return resumable;
    }
    
    private Path getResumePath(String fileHash) {
        return Paths.get(resumeDir, fileHash + RESUME_FILE_EXT);
    }
}

