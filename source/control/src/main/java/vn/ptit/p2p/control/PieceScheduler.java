package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schedules piece downloads from peers using rarest-first or sequential strategy
 */
public class PieceScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PieceScheduler.class);
    
    public enum ScheduleMode {
        SEQUENTIAL,      // Tải từ piece 0 → n (tốt cho streaming)
        RAREST_FIRST     // Tải piece hiếm nhất trước (tốt cho swarm)
    }
    
    private final ManifestStore manifestStore;
    private final Map<String, Map<Integer, Integer>> pieceRarity = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> requestedPieces = new ConcurrentHashMap<>();
    private final Map<String, ScheduleMode> fileModes = new ConcurrentHashMap<>();
    
    public PieceScheduler(ManifestStore manifestStore) {
        this.manifestStore = manifestStore;
    }
    
    /**
     * Đặt chế độ lập lịch cho file
     */
    public void setScheduleMode(String fileHash, ScheduleMode mode) {
        fileModes.put(fileHash, mode);
        logger.info("Set schedule mode for {}: {}", fileHash, mode);
    }
    
    /**
     * Lấy chế độ lập lịch hiện tại
     */
    public ScheduleMode getScheduleMode(String fileHash) {
        return fileModes.getOrDefault(fileHash, ScheduleMode.RAREST_FIRST);
    }
    
    /**
     * Update piece availability for a peer
     */
    public void updatePeerPieces(String fileHash, String peerId, BitSet pieces) {
        Map<Integer, Integer> rarity = pieceRarity.computeIfAbsent(fileHash, k -> new ConcurrentHashMap<>());
        
        // Update rarity count for each piece
        for (int i = pieces.nextSetBit(0); i >= 0; i = pieces.nextSetBit(i + 1)) {
            rarity.merge(i, 1, Integer::sum);
        }
        
        logger.debug("Updated piece availability for file {} from peer {}", fileHash, peerId);
    }
    
    /**
     * Get next pieces to download using configured strategy
     */
    public List<Integer> getNextPieces(String fileHash, int count) {
        ScheduleMode mode = getScheduleMode(fileHash);
        
        if (mode == ScheduleMode.SEQUENTIAL) {
            return getNextPiecesSequential(fileHash, count);
        } else {
            return getNextPiecesRarestFirst(fileHash, count);
        }
    }
    
    /**
     * Sequential scheduling: tải từ piece 0 → n
     */
    private List<Integer> getNextPiecesSequential(String fileHash, int count) {
        List<Integer> missing = manifestStore.getMissingPieces(fileHash);
        Set<Integer> requested = requestedPieces.getOrDefault(fileHash, Collections.emptySet());
        
        // Filter out already requested pieces
        List<Integer> available = new ArrayList<>();
        for (Integer piece : missing) {
            if (!requested.contains(piece)) {
                available.add(piece);
            }
        }
        
        // Sort by piece index (sequential)
        Collections.sort(available);
        
        // Take up to 'count' pieces
        List<Integer> result = available.subList(0, Math.min(count, available.size()));
        
        // Mark as requested
        Set<Integer> reqSet = requestedPieces.computeIfAbsent(fileHash, k -> ConcurrentHashMap.newKeySet());
        reqSet.addAll(result);
        
        logger.debug("Scheduled {} pieces (SEQUENTIAL) for {}", result.size(), fileHash);
        return result;
    }
    
    /**
     * Rarest-first scheduling: tải piece hiếm nhất trước
     */
    private List<Integer> getNextPiecesRarestFirst(String fileHash, int count) {
        List<Integer> missing = manifestStore.getMissingPieces(fileHash);
        Set<Integer> requested = requestedPieces.getOrDefault(fileHash, Collections.emptySet());
        Map<Integer, Integer> rarity = pieceRarity.getOrDefault(fileHash, Collections.emptyMap());
        
        // Filter out already requested pieces
        List<Integer> available = new ArrayList<>();
        for (Integer piece : missing) {
            if (!requested.contains(piece)) {
                available.add(piece);
            }
        }
        
        // Sort by rarity (rarest first)
        available.sort(Comparator.comparingInt(p -> rarity.getOrDefault(p, 0)));
        
        // Take up to 'count' pieces
        List<Integer> result = available.subList(0, Math.min(count, available.size()));
        
        // Mark as requested
        Set<Integer> reqSet = requestedPieces.computeIfAbsent(fileHash, k -> ConcurrentHashMap.newKeySet());
        reqSet.addAll(result);
        
        logger.debug("Scheduled {} pieces (RAREST_FIRST) for {}", result.size(), fileHash);
        return result;
    }
    
    /**
     * Mark a piece as completed (no longer needs to be requested)
     */
    public void markPieceCompleted(String fileHash, int pieceIndex) {
        Set<Integer> requested = requestedPieces.get(fileHash);
        if (requested != null) {
            requested.remove(pieceIndex);
        }
        logger.debug("Marked piece {} of file {} as completed", pieceIndex, fileHash);
    }
    
    /**
     * Mark a piece request as failed (can be requested again)
     */
    public void markPieceFailed(String fileHash, int pieceIndex) {
        Set<Integer> requested = requestedPieces.get(fileHash);
        if (requested != null) {
            requested.remove(pieceIndex);
        }
        logger.debug("Marked piece {} of file {} as failed", pieceIndex, fileHash);
    }
    
    /**
     * Clear all scheduled pieces for a file
     */
    public void clearSchedule(String fileHash) {
        requestedPieces.remove(fileHash);
        pieceRarity.remove(fileHash);
        logger.debug("Cleared schedule for file {}", fileHash);
    }
}

