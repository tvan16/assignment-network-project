package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.FileMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage for file manifests and piece availability
 */
public class ManifestStore {
    private static final Logger logger = LoggerFactory.getLogger(ManifestStore.class);
    
    private final Map<String, FileMetadata> manifests = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> peersByFile = new ConcurrentHashMap<>();
    private final Map<String, BitSet> pieceAvailability = new ConcurrentHashMap<>();
    
    /**
     * Add a file manifest
     */
    public void addManifest(String fileHash, FileMetadata metadata) {
        manifests.put(fileHash, metadata);
        pieceAvailability.putIfAbsent(fileHash, new BitSet(metadata.getPieceCount()));
        logger.info("Added manifest for file: {} ({})", metadata.getFileName(), fileHash);
    }
    
    /**
     * Get a file manifest
     */
    public FileMetadata getManifest(String fileHash) {
        return manifests.get(fileHash);
    }
    
    /**
     * Check if a manifest exists
     */
    public boolean hasManifest(String fileHash) {
        return manifests.containsKey(fileHash);
    }
    
    /**
     * Register a peer as having a file
     */
    public void registerPeerForFile(String fileHash, String peerId) {
        peersByFile.computeIfAbsent(fileHash, k -> ConcurrentHashMap.newKeySet()).add(peerId);
        logger.debug("Registered peer {} for file {}", peerId, fileHash);
    }
    
    /**
     * Get all peers that have a file
     */
    public Set<String> getPeersForFile(String fileHash) {
        return new HashSet<>(peersByFile.getOrDefault(fileHash, Collections.emptySet()));
    }
    
    /**
     * Mark a piece as available
     */
    public void markPieceAvailable(String fileHash, int pieceIndex) {
        BitSet bitSet = pieceAvailability.get(fileHash);
        if (bitSet != null) {
            bitSet.set(pieceIndex);
            logger.debug("Marked piece {} of file {} as available", pieceIndex, fileHash);
        }
    }
    
    /**
     * Check if a piece is available
     */
    public boolean isPieceAvailable(String fileHash, int pieceIndex) {
        BitSet bitSet = pieceAvailability.get(fileHash);
        return bitSet != null && bitSet.get(pieceIndex);
    }
    
    /**
     * Get all available pieces for a file
     */
    public BitSet getAvailablePieces(String fileHash) {
        return (BitSet) pieceAvailability.getOrDefault(fileHash, new BitSet()).clone();
    }
    
    /**
     * Get missing pieces for a file
     */
    public List<Integer> getMissingPieces(String fileHash) {
        FileMetadata metadata = manifests.get(fileHash);
        if (metadata == null) {
            return Collections.emptyList();
        }
        
        BitSet available = pieceAvailability.getOrDefault(fileHash, new BitSet());
        List<Integer> missing = new ArrayList<>();
        
        for (int i = 0; i < metadata.getPieceCount(); i++) {
            if (!available.get(i)) {
                missing.add(i);
            }
        }
        
        return missing;
    }
    
    /**
     * Check if file is complete
     */
    public boolean isFileComplete(String fileHash) {
        return getMissingPieces(fileHash).isEmpty();
    }
    
    /**
     * Get all file hashes
     */
    public Set<String> getAllFileHashes() {
        return new HashSet<>(manifests.keySet());
    }
    
    /**
     * Verify SHA256 của toàn bộ file từ các pieces đã tải
     * @param fileHash Hash mong đợi
     * @param completedPieces Dữ liệu của tất cả pieces đã ghép
     * @return true nếu hash khớp
     */
    public boolean verifySha256(String fileHash, byte[] completedPieces) {
        try {
            String actualHash = vn.ptit.p2p.common.Hashing.hashBytes(completedPieces);
            boolean matches = actualHash.equalsIgnoreCase(fileHash);
            
            if (matches) {
                logger.info("SHA256 verification successful for file {}", fileHash);
            } else {
                logger.error("SHA256 verification FAILED for file {}. Expected: {}, Got: {}", 
                           fileHash, fileHash, actualHash);
            }
            
            return matches;
        } catch (Exception e) {
            logger.error("Error verifying SHA256", e);
            return false;
        }
    }
    
    /**
     * Tính hash SHA256 từ danh sách pieces
     * @param fileHash Hash của file
     * @param piecesData Map của pieceIndex -> pieceData
     * @return SHA256 hash của file hoàn chỉnh
     */
    public String computeFileHash(String fileHash, Map<Integer, byte[]> piecesData) {
        FileMetadata metadata = getManifest(fileHash);
        if (metadata == null) {
            logger.error("No manifest found for file {}", fileHash);
            return null;
        }
        
        try {
            // Ghép tất cả pieces theo thứ tự
            int totalSize = 0;
            for (byte[] pieceData : piecesData.values()) {
                totalSize += pieceData.length;
            }
            
            byte[] completeFile = new byte[totalSize];
            int offset = 0;
            
            for (int i = 0; i < metadata.getPieceCount(); i++) {
                byte[] pieceData = piecesData.get(i);
                if (pieceData == null) {
                    logger.error("Missing piece {} for file {}", i, fileHash);
                    return null;
                }
                
                System.arraycopy(pieceData, 0, completeFile, offset, pieceData.length);
                offset += pieceData.length;
            }
            
            // Tính hash
            String computedHash = vn.ptit.p2p.common.Hashing.hashBytes(completeFile);
            logger.info("Computed file hash: {}", computedHash);
            
            return computedHash;
            
        } catch (Exception e) {
            logger.error("Error computing file hash", e);
            return null;
        }
    }
    
    /**
     * Verify từng piece hash
     * @param fileHash Hash của file
     * @param pieceIndex Index của piece
     * @param pieceData Dữ liệu piece
     * @return true nếu hash khớp
     */
    public boolean verifyPieceHash(String fileHash, int pieceIndex, byte[] pieceData) {
        FileMetadata metadata = getManifest(fileHash);
        if (metadata == null) {
            return false;
        }
        
        List<String> pieceHashes = metadata.getPieceHashes();
        if (pieceIndex < 0 || pieceIndex >= pieceHashes.size()) {
            return false;
        }
        
        String expectedHash = pieceHashes.get(pieceIndex);
        String actualHash = vn.ptit.p2p.common.Hashing.hashBytes(pieceData);
        
        return actualHash.equalsIgnoreCase(expectedHash);
    }
}

