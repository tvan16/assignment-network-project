package vn.ptit.p2p.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Piece;
import vn.ptit.p2p.common.Models.Peer;
import vn.ptit.p2p.dataapi.DataApi;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main data transfer service that coordinates UDP and TCP transfers
 */
public class DataService implements DataApi {
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    
    private final UdpDataSender udpSender;
    private final UdpDataReceiver udpReceiver;
    private final TcpFallbackSender tcpSender;
    
    private PieceRequestHandler requestHandler;
    
    // Statistics
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicInteger activeSends = new AtomicInteger(0);
    private final AtomicInteger activeReceives = new AtomicInteger(0);
    private final AtomicInteger totalSends = new AtomicInteger(0);
    private final AtomicInteger totalReceives = new AtomicInteger(0);
    private final AtomicInteger failedSends = new AtomicInteger(0);
    private final AtomicInteger failedReceives = new AtomicInteger(0);
    
    public DataService() {
        this.udpSender = new UdpDataSender(this);
        this.udpReceiver = new UdpDataReceiver(this);
        this.tcpSender = new TcpFallbackSender(this);
    }
    
    @Override
    public CompletableFuture<Void> sendPiece(Peer peer, Piece piece) {
        logger.debug("Sending piece {} of file {} to peer {}", 
                    piece.getIndex(), piece.getFileHash(), peer.getId());
        
        activeSends.incrementAndGet();
        totalSends.incrementAndGet();
        
        return udpSender.sendPiece(peer, piece)
            .handle((result, error) -> {
                activeSends.decrementAndGet();
                
                if (error != null) {
                    logger.warn("UDP send failed, trying TCP fallback");
                    failedSends.incrementAndGet();
                    
                    // Try TCP fallback
                    return tcpSender.sendPiece(peer, piece)
                        .handle((tcpResult, tcpError) -> {
                            if (tcpError != null) {
                                failedSends.incrementAndGet();
                                throw new RuntimeException("Failed to send piece", tcpError);
                            }
                            bytesSent.addAndGet(piece.getData().length);
                            return null;
                        }).join();
                }
                
                bytesSent.addAndGet(piece.getData().length);
                return null;
            });
    }
    
    @Override
    public CompletableFuture<Piece> requestPiece(Peer peer, String fileHash, int pieceIndex) {
        logger.debug("Requesting piece {} of file {} from peer {}", 
                    pieceIndex, fileHash, peer.getId());
        
        activeReceives.incrementAndGet();
        totalReceives.incrementAndGet();
        
        return udpReceiver.requestPiece(peer, fileHash, pieceIndex)
            .handle((piece, error) -> {
                activeReceives.decrementAndGet();
                
                if (error != null) {
                    logger.warn("UDP receive failed, trying TCP fallback");
                    failedReceives.incrementAndGet();
                    
                    // Try TCP fallback
                    return tcpSender.requestPiece(peer, fileHash, pieceIndex)
                        .handle((tcpPiece, tcpError) -> {
                            if (tcpError != null) {
                                failedReceives.incrementAndGet();
                                throw new RuntimeException("Failed to receive piece", tcpError);
                            }
                            if (tcpPiece != null) {
                                bytesReceived.addAndGet(tcpPiece.getData().length);
                            }
                            return tcpPiece;
                        }).join();
                }
                
                if (piece != null) {
                    bytesReceived.addAndGet(piece.getData().length);
                }
                return piece;
            });
    }
    
    @Override
    public void startListening(int port) throws IOException {
        logger.info("Starting data service on port {}", port);
        udpReceiver.start(port);
        tcpSender.startServer(port + 1);
    }
    
    @Override
    public void stopListening() {
        logger.info("Stopping data service");
        udpReceiver.stop();
        tcpSender.stopServer();
    }
    
    @Override
    public void registerPieceRequestHandler(PieceRequestHandler handler) {
        this.requestHandler = handler;
    }
    
    @Override
    public DataTransferStats getStats() {
        return new DataTransferStats(
            bytesSent.get(),
            bytesReceived.get(),
            activeSends.get(),
            activeReceives.get(),
            totalSends.get(),
            totalReceives.get(),
            failedSends.get(),
            failedReceives.get()
        );
    }
    
    /**
     * Handle incoming piece request
     */
    public Piece handleIncomingRequest(Peer peer, String fileHash, int pieceIndex) {
        if (requestHandler != null) {
            return requestHandler.handlePieceRequest(peer, fileHash, pieceIndex);
        }
        return null;
    }
}

