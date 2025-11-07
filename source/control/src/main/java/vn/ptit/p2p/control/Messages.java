package vn.ptit.p2p.control;

import java.util.List;
import java.util.Map;

/**
 * Control protocol message definitions
 */
public class Messages {
    
    public static class BaseMessage {
        private String messageType;
        private String peerId;
        private long timestamp;
        
        public BaseMessage() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        public String getPeerId() { return peerId; }
        public void setPeerId(String peerId) { this.peerId = peerId; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    public static class OfferFile extends BaseMessage {
        private String fileHash;
        private String fileName;
        private long fileSize;
        private int pieceSize;
        private int pieceCount;
        private List<String> pieceHashes;
        
        public OfferFile() {
            setMessageType("offer_file");
        }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public int getPieceSize() { return pieceSize; }
        public void setPieceSize(int pieceSize) { this.pieceSize = pieceSize; }
        
        public int getPieceCount() { return pieceCount; }
        public void setPieceCount(int pieceCount) { this.pieceCount = pieceCount; }
        
        public List<String> getPieceHashes() { return pieceHashes; }
        public void setPieceHashes(List<String> pieceHashes) { this.pieceHashes = pieceHashes; }
    }
    
    public static class RequestPieces extends BaseMessage {
        private String fileHash;
        private List<Integer> pieces;
        
        public RequestPieces() {
            setMessageType("request_pieces");
        }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public List<Integer> getPieces() { return pieces; }
        public void setPieces(List<Integer> pieces) { this.pieces = pieces; }
    }
    
    public static class Have extends BaseMessage {
        private String fileHash;
        private List<Integer> pieces;
        private String bitfield;
        
        public Have() {
            setMessageType("have");
        }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public List<Integer> getPieces() { return pieces; }
        public void setPieces(List<Integer> pieces) { this.pieces = pieces; }
        
        public String getBitfield() { return bitfield; }
        public void setBitfield(String bitfield) { this.bitfield = bitfield; }
    }
    
    public static class Nack extends BaseMessage {
        private String requestType;
        private String fileHash;
        private String reason;
        private String message;
        
        public Nack() {
            setMessageType("nack");
        }
        
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class PingPong extends BaseMessage {
        private int sequence;
        private Map<String, Object> stats;
        
        public PingPong() {}
        
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
        
        public Map<String, Object> getStats() { return stats; }
        public void setStats(Map<String, Object> stats) { this.stats = stats; }
    }
}

