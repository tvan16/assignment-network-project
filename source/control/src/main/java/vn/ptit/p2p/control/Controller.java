package vn.ptit.p2p.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Json;
import vn.ptit.p2p.common.Models.FileMetadata;
import vn.ptit.p2p.common.Models.Peer;
import vn.ptit.p2p.dataapi.DataApi;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main controller that coordinates all control protocol operations
 * Điều phối giữa Control Plane (B) và Data Plane (C)
 */
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);
    
    private final String peerId;
    private final ManifestStore manifestStore;
    private final PieceScheduler pieceScheduler;
    private final ResumeManager resumeManager;
    private final ControlEvents events;
    
    private ControlServer server;
    private ControlClient client;
    private DataApi dataApi;  // Interface gọi sang Data Plane (Người C)
    
    // Tracking active transfers
    private final Map<String, TransferSession> activeSessions = new ConcurrentHashMap<>();
    
    public Controller(String peerId, ManifestStore manifestStore, 
                     PieceScheduler pieceScheduler, ResumeManager resumeManager) {
        this.peerId = peerId;
        this.manifestStore = manifestStore;
        this.pieceScheduler = pieceScheduler;
        this.resumeManager = resumeManager;
        this.events = new ControlEvents();
    }
    
    /**
     * Set Data API để giao tiếp với Data Plane (Người C)
     */
    public void setDataApi(DataApi dataApi) {
        this.dataApi = dataApi;
        logger.info("Data API registered with Controller");
    }
    
    /**
     * Initialize the controller
     */
    public void initialize(ControlConfig config) throws Exception {
        this.server = new ControlServer(config, this);
        this.client = new ControlClient();
        
        server.start();
        logger.info("Controller initialized");
    }
    
    /**
     * Shutdown the controller
     */
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
        logger.info("Controller shutdown");
    }
    
    /**
     * Handle incoming message
     */
    @SuppressWarnings("unchecked")
    public String handleMessage(String messageJson, Socket socket) {
        try {
            Map<String, Object> message = Json.fromJson(messageJson, Map.class);
            String messageType = (String) message.get("message_type");
            
            logger.debug("Handling message type: {}", messageType);
            
            switch (messageType) {
                case "offer_file":
                    return handleOfferFile(message);
                case "request_pieces":
                    return handleRequestPieces(message);
                case "have":
                    return handleHave(message);
                case "ping":
                    return handlePing(message);
                case "nack":
                    return handleNack(message);
                default:
                    logger.warn("Unknown message type: {}", messageType);
                    return createNackResponse("invalid_request", "Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error handling message", e);
            return createNackResponse("internal_error", e.getMessage());
        }
    }
    
    private String handleOfferFile(Map<String, Object> message) {
        String fileHash = (String) message.get("file_hash");
        String fileName = (String) message.get("file_name");
        // Handle offer file logic
        logger.info("Received file offer: {} ({})", fileName, fileHash);
        return "{\"status\": \"ok\"}";
    }
    
    private String handleRequestPieces(Map<String, Object> message) {
        String fileHash = (String) message.get("file_hash");
        // Handle piece request logic
        logger.debug("Received piece request for file: {}", fileHash);
        return "{\"status\": \"ok\"}";
    }
    
    private String handleHave(Map<String, Object> message) {
        String fileHash = (String) message.get("file_hash");
        // Handle have message logic
        logger.debug("Received have message for file: {}", fileHash);
        return "{\"status\": \"ok\"}";
    }
    
    private String handlePing(Map<String, Object> message) {
        // Create pong response
        Messages.PingPong pong = new Messages.PingPong();
        pong.setMessageType("pong");
        pong.setPeerId(peerId);
        pong.setSequence(((Number) message.get("sequence")).intValue());
        
        return Json.toJson(pong);
    }
    
    private String handleNack(Map<String, Object> message) {
        String reason = (String) message.get("reason");
        logger.warn("Received NACK: {}", reason);
        return "{\"status\": \"acknowledged\"}";
    }
    
    private String createNackResponse(String reason, String message) {
        Messages.Nack nack = new Messages.Nack();
        nack.setPeerId(peerId);
        nack.setReason(reason);
        nack.setMessage(message);
        
        return Json.toJson(nack);
    }
    
    public ManifestStore getManifestStore() {
        return manifestStore;
    }
    
    public PieceScheduler getPieceScheduler() {
        return pieceScheduler;
    }
    
    public ResumeManager getResumeManager() {
        return resumeManager;
    }
    
    public ControlEvents getEvents() {
        return events;
    }
    
    public ControlClient getClient() {
        return client;
    }
    
    // ===================================================================
    // API GỌI SANG DATA PLANE (NGƯỜI C)
    // ===================================================================
    
    /**
     * Gửi piece qua Data Plane
     * @param fileHash Hash của file
     * @param pieceId Index của piece
     * @param peer Peer đích
     */
    public void sendPiece(String fileHash, int pieceId, Peer peer) {
        if (dataApi == null) {
            logger.error("Data API not initialized!");
            return;
        }
        
        logger.debug("Requesting Data Plane to send piece {} of {} to {}", 
                    pieceId, fileHash, peer.getId());
        
        // Lấy piece data từ manifest/storage
        // Trong thực tế cần implement piece storage
        // dataApi.sendPiece(peer, piece);
    }
    
    /**
     * Yêu cầu retransmit các sequence cụ thể
     * @param fileHash Hash của file
     * @param pieceId Index của piece
     * @param seqList Danh sách sequence number cần retransmit
     */
    public void retransmitSeq(String fileHash, int pieceId, List<Integer> seqList) {
        if (dataApi == null) {
            logger.error("Data API not initialized!");
            return;
        }
        
        logger.debug("Requesting retransmit {} sequences for piece {} of {}", 
                    seqList.size(), pieceId, fileHash);
        
        // Gọi API của Data Plane để retransmit
        // dataApi.retransmitSeq(fileHash, pieceId, seqList);
    }
    
    /**
     * Hủy transfer đang diễn ra
     * @param fileHash Hash của file cần hủy
     */
    public void cancelTransfer(String fileHash) {
        logger.info("Cancelling transfer for file {}", fileHash);
        
        TransferSession session = activeSessions.remove(fileHash);
        if (session != null) {
            session.cancel();
        }
        
        pieceScheduler.clearSchedule(fileHash);
    }
    
    /**
     * Fallback sang TCP cho piece xấu
     * @param fileHash Hash của file
     * @param pieceId Index của piece
     * @param peer Peer nguồn
     */
    public void fallbackTcp(String fileHash, int pieceId, Peer peer) {
        logger.warn("Falling back to TCP for piece {} of {} from {}", 
                   pieceId, fileHash, peer.getId());
        
        if (dataApi == null) {
            logger.error("Data API not initialized!");
            return;
        }
        
        // Yêu cầu Data Plane chuyển sang TCP cho piece này
        // dataApi.requestPiece(peer, fileHash, pieceId);  // Sẽ tự động fallback TCP
    }
    
    // ===================================================================
    // EVENT HANDLERS TỪ DATA PLANE (NGƯỜI C)
    // ===================================================================
    
    /**
     * Callback khi Data Plane hoàn thành 1 piece
     * @param fileHash Hash của file
     * @param pieceId Index của piece
     */
    public void onPieceDone(String fileHash, int pieceId) {
        logger.info("Piece {} of {} completed", pieceId, fileHash);
        
        // Đánh dấu piece hoàn thành
        manifestStore.markPieceAvailable(fileHash, pieceId);
        pieceScheduler.markPieceCompleted(fileHash, pieceId);
        
        // Lưu checkpoint để resume
        FileMetadata metadata = manifestStore.getManifest(fileHash);
        if (metadata != null) {
            resumeManager.saveDownloadState(
                fileHash, 
                manifestStore.getAvailablePieces(fileHash),
                null  // output path
            );
        }
        
        // Fire event
        int totalPieces = metadata != null ? metadata.getPieceCount() : 0;
        events.firePieceReceived(fileHash, pieceId, totalPieces);
        
        // Kiểm tra nếu file đã hoàn thành
        if (manifestStore.isFileComplete(fileHash)) {
            onFileCompleted(fileHash);
        } else {
            // Lập lịch piece tiếp theo
            scheduleNextPieces(fileHash);
        }
    }
    
    /**
     * Callback khi Data Plane phát hiện loss cao
     * @param fileHash Hash của file
     * @param pieceId Index của piece
     * @param lossRate Tỷ lệ loss (0.0 - 1.0)
     */
    public void onLossAlert(String fileHash, int pieceId, double lossRate) {
        logger.warn("High loss detected for piece {} of {}: {}%", 
                   pieceId, fileHash, lossRate * 100);
        
        // Nếu loss quá cao (>30%), chuyển sang TCP
        if (lossRate > 0.3) {
            logger.warn("Loss rate too high, switching to TCP fallback");
            // fallbackTcp sẽ được gọi tự động bởi Data Plane
        } else if (lossRate > 0.1) {
            // Loss vừa phải, yêu cầu retransmit
            logger.info("Requesting retransmission due to packet loss");
            // Data Plane sẽ tự động retransmit theo cơ chế của nó
        }
    }
    
    /**
     * Callback khi Data Plane phát hiện lỗi CRC
     * @param fileHash Hash của file
     * @param pieceId Index của piece bị lỗi
     */
    public void onPieceCrcError(String fileHash, int pieceId) {
        logger.error("CRC error detected for piece {} of {}", pieceId, fileHash);
        
        // Đánh dấu piece thất bại
        pieceScheduler.markPieceFailed(fileHash, pieceId);
        
        // Yêu cầu tải lại piece này
        logger.info("Re-requesting piece {} due to CRC error", pieceId);
        scheduleRetryPiece(fileHash, pieceId);
    }
    
    /**
     * Lập lịch tải các piece tiếp theo
     */
    private void scheduleNextPieces(String fileHash) {
        List<Integer> nextPieces = pieceScheduler.getNextPieces(fileHash, 5);  // Lấy 5 pieces
        
        if (!nextPieces.isEmpty()) {
            logger.debug("Scheduling {} more pieces for {}", nextPieces.size(), fileHash);
            // Gửi REQUEST_PIECES tới peers
            // Implementation sẽ gửi message REQUEST_PIECES qua ControlClient
        }
    }
    
    /**
     * Lập lịch retry cho piece bị lỗi
     */
    private void scheduleRetryPiece(String fileHash, int pieceId) {
        logger.debug("Scheduling retry for piece {} of {}", pieceId, fileHash);
        // Gửi lại REQUEST_PIECES cho piece này
    }
    
    /**
     * Xử lý khi file hoàn thành
     */
    private void onFileCompleted(String fileHash) {
        logger.info("File {} download completed!", fileHash);
        
        FileMetadata metadata = manifestStore.getManifest(fileHash);
        if (metadata == null) {
            return;
        }
        
        // Verify SHA256 toàn file
        boolean verified = verifyCompleteFile(fileHash);
        
        if (verified) {
            logger.info("File {} verified successfully", fileHash);
            
            // Xóa resume state
            resumeManager.deleteResumeState(fileHash);
            
            // Fire event
            events.fireDownloadCompleted(fileHash, null);
            
            // Dọn dẹp session
            activeSessions.remove(fileHash);
        } else {
            logger.error("File {} verification failed! Re-downloading corrupted pieces", fileHash);
            
            // TODO: Xác định pieces nào bị lỗi và tải lại
            events.fireDownloadFailed(fileHash, "Verification failed");
        }
    }
    
    /**
     * Verify SHA256 của toàn bộ file
     */
    private boolean verifyCompleteFile(String fileHash) {
        // TODO: Implement full file verification
        // 1. Ghép tất cả pieces lại
        // 2. Tính SHA256 của file hoàn chỉnh
        // 3. So sánh với fileHash
        
        logger.debug("Verifying complete file {}", fileHash);
        return true;  // Placeholder
    }
    
    /**
     * Inner class để track transfer session
     */
    private static class TransferSession {
        private final String fileHash;
        private volatile boolean cancelled = false;
        
        public TransferSession(String fileHash) {
            this.fileHash = fileHash;
        }
        
        public void cancel() {
            cancelled = true;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
    }
}

