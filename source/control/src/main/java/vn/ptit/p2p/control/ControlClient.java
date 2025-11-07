package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Peer;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Client for sending control messages to peers with auto-reconnect
 */
public class ControlClient {
    private static final Logger logger = LoggerFactory.getLogger(ControlClient.class);
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;
    
    // Connection pool để tái sử dụng kết nối
    private final Map<String, Socket> connectionPool = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(2);
    
    /**
     * Send a message to a peer với auto-retry
     */
    public CompletableFuture<String> sendMessage(Peer peer, Object message) {
        return sendMessageWithRetry(peer, message, 0);
    }
    
    /**
     * Send message với retry logic
     */
    private CompletableFuture<String> sendMessageWithRetry(Peer peer, Object message, int attemptCount) {
        return CompletableFuture.supplyAsync(() -> {
            Socket socket = null;
            try {
                // Thử lấy connection từ pool
                socket = getOrCreateConnection(peer);
                
                return TcpJsonCodec.sendAndReceive(socket, message);
                
            } catch (IOException e) {
                logger.warn("Failed to send message to peer {} (attempt {}/{})", 
                           peer.getId(), attemptCount + 1, MAX_RETRY_ATTEMPTS);
                
                // Đóng connection lỗi
                closeConnection(peer.getId());
                
                // Retry nếu chưa đạt max attempts
                if (attemptCount < MAX_RETRY_ATTEMPTS - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (attemptCount + 1));  // Exponential backoff
                        return sendMessageWithRetry(peer, message, attemptCount + 1).join();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                logger.error("Failed to send message to peer {} after {} attempts", 
                           peer.getId(), MAX_RETRY_ATTEMPTS);
                throw new RuntimeException("Failed to send message after retries", e);
            }
        });
    }
    
    /**
     * Lấy hoặc tạo connection mới
     */
    private Socket getOrCreateConnection(Peer peer) throws IOException {
        Socket socket = connectionPool.get(peer.getId());
        
        // Kiểm tra connection còn sống không
        if (socket != null && !socket.isClosed() && socket.isConnected()) {
            return socket;
        }
        
        // Tạo connection mới
        logger.debug("Creating new connection to peer {}", peer.getId());
        socket = new Socket(peer.getHost(), peer.getPort());
        socket.setSoTimeout(CONNECTION_TIMEOUT);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        
        connectionPool.put(peer.getId(), socket);
        return socket;
    }
    
    /**
     * Đóng connection
     */
    private void closeConnection(String peerId) {
        Socket socket = connectionPool.remove(peerId);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Error closing connection", e);
            }
        }
    }
    
    /**
     * Đóng tất cả connections
     */
    public void closeAll() {
        logger.info("Closing all connections");
        for (Map.Entry<String, Socket> entry : connectionPool.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                logger.debug("Error closing connection to {}", entry.getKey(), e);
            }
        }
        connectionPool.clear();
        reconnectExecutor.shutdown();
    }
    
    /**
     * Send a message without expecting a response
     */
    public void sendMessageAsync(Peer peer, Object message) {
        CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket(peer.getHost(), peer.getPort())) {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                TcpJsonCodec.sendMessage(socket, message);
            } catch (IOException e) {
                logger.error("Failed to send async message to peer {}", peer.getId(), e);
            }
        });
    }
    
    /**
     * Send offer file message
     */
    public CompletableFuture<String> sendOfferFile(Peer peer, Messages.OfferFile offer) {
        return sendMessage(peer, offer);
    }
    
    /**
     * Send request pieces message
     */
    public CompletableFuture<String> sendRequestPieces(Peer peer, Messages.RequestPieces request) {
        return sendMessage(peer, request);
    }
    
    /**
     * Send have message
     */
    public void sendHave(Peer peer, Messages.Have have) {
        sendMessageAsync(peer, have);
    }
    
    /**
     * Send ping message
     */
    public CompletableFuture<String> sendPing(Peer peer, Messages.PingPong ping) {
        return sendMessage(peer, ping);
    }
}

