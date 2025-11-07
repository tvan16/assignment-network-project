package vn.ptit.p2p.dataapi;

import vn.ptit.p2p.common.Models.Piece;
import vn.ptit.p2p.common.Models.Peer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for data transfer operations
 */
public interface DataApi {
    
    /**
     * Send a piece to a peer
     * 
     * @param peer The peer to send the piece to
     * @param piece The piece to send
     * @return CompletableFuture that completes when the piece is sent
     */
    CompletableFuture<Void> sendPiece(Peer peer, Piece piece);
    
    /**
     * Request a piece from a peer
     * 
     * @param peer The peer to request from
     * @param fileHash The hash of the file
     * @param pieceIndex The index of the piece to request
     * @return CompletableFuture that completes with the received piece
     */
    CompletableFuture<Piece> requestPiece(Peer peer, String fileHash, int pieceIndex);
    
    /**
     * Start listening for incoming data connections
     * 
     * @param port The port to listen on
     * @throws IOException If unable to bind to the port
     */
    void startListening(int port) throws IOException;
    
    /**
     * Stop listening for incoming connections
     */
    void stopListening();
    
    /**
     * Register a handler for incoming piece requests
     * 
     * @param handler The handler to process incoming requests
     */
    void registerPieceRequestHandler(PieceRequestHandler handler);
    
    /**
     * Get transfer statistics
     * 
     * @return DataTransferStats object with current statistics
     */
    DataTransferStats getStats();
    
    /**
     * Handler interface for incoming piece requests
     */
    interface PieceRequestHandler {
        /**
         * Handle a piece request from a peer
         * 
         * @param peer The peer requesting the piece
         * @param fileHash The hash of the file
         * @param pieceIndex The index of the piece
         * @return The piece, or null if not available
         */
        Piece handlePieceRequest(Peer peer, String fileHash, int pieceIndex);
    }
    
    /**
     * Data transfer statistics
     */
    class DataTransferStats {
        private final long bytesSent;
        private final long bytesReceived;
        private final int activeSends;
        private final int activeReceives;
        private final int totalSends;
        private final int totalReceives;
        private final int failedSends;
        private final int failedReceives;
        
        public DataTransferStats(long bytesSent, long bytesReceived, 
                               int activeSends, int activeReceives,
                               int totalSends, int totalReceives,
                               int failedSends, int failedReceives) {
            this.bytesSent = bytesSent;
            this.bytesReceived = bytesReceived;
            this.activeSends = activeSends;
            this.activeReceives = activeReceives;
            this.totalSends = totalSends;
            this.totalReceives = totalReceives;
            this.failedSends = failedSends;
            this.failedReceives = failedReceives;
        }
        
        public long getBytesSent() { return bytesSent; }
        public long getBytesReceived() { return bytesReceived; }
        public int getActiveSends() { return activeSends; }
        public int getActiveReceives() { return activeReceives; }
        public int getTotalSends() { return totalSends; }
        public int getTotalReceives() { return totalReceives; }
        public int getFailedSends() { return failedSends; }
        public int getFailedReceives() { return failedReceives; }
    }
}

