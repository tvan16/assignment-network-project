package vn.ptit.p2p.control;

import vn.ptit.p2p.common.Models.FileMetadata;
import vn.ptit.p2p.common.Models.Peer;

import java.util.ArrayList;
import java.util.List;

/**
 * Event system for control protocol events
 */
public class ControlEvents {
    
    private final List<ControlEventListener> listeners = new ArrayList<>();
    
    /**
     * Add an event listener
     */
    public void addListener(ControlEventListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove an event listener
     */
    public void removeListener(ControlEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Fire file offered event
     */
    public void fireFileOffered(Peer peer, FileMetadata metadata) {
        for (ControlEventListener listener : listeners) {
            try {
                listener.onFileOffered(peer, metadata);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }
    
    /**
     * Fire piece received event
     */
    public void firePieceReceived(String fileHash, int pieceIndex, int totalPieces) {
        for (ControlEventListener listener : listeners) {
            try {
                listener.onPieceReceived(fileHash, pieceIndex, totalPieces);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }
    
    /**
     * Fire download completed event
     */
    public void fireDownloadCompleted(String fileHash, String outputPath) {
        for (ControlEventListener listener : listeners) {
            try {
                listener.onDownloadCompleted(fileHash, outputPath);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }
    
    /**
     * Fire download failed event
     */
    public void fireDownloadFailed(String fileHash, String reason) {
        for (ControlEventListener listener : listeners) {
            try {
                listener.onDownloadFailed(fileHash, reason);
            } catch (Exception e) {
                // Log but don't propagate
            }
        }
    }
    
    /**
     * Interface for control event listeners
     */
    public interface ControlEventListener {
        void onFileOffered(Peer peer, FileMetadata metadata);
        void onPieceReceived(String fileHash, int pieceIndex, int totalPieces);
        void onDownloadCompleted(String fileHash, String outputPath);
        void onDownloadFailed(String fileHash, String reason);
    }
}

