package vn.ptit.p2p.common;

import java.util.List;
import java.util.Objects;

/**
 * Common data models used across the P2P application
 */
public class Models {
    
    /**
     * Represents a peer in the network
     */
    public static class Peer {
        private String id;
        private String name;
        private String host;
        private int port;
        private long lastSeen;
        
        public Peer(String id, String name, String host, int port) {
            this.id = id;
            this.name = name;
            this.host = host;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public long getLastSeen() { return lastSeen; }
        
        public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Peer peer = (Peer) o;
            return Objects.equals(id, peer.id);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
    
    /**
     * Represents file metadata
     */
    public static class FileMetadata {
        private String fileHash;
        private String fileName;
        private long fileSize;
        private int pieceSize;
        private int pieceCount;
        private List<String> pieceHashes;
        
        public FileMetadata(String fileHash, String fileName, long fileSize, 
                           int pieceSize, int pieceCount, List<String> pieceHashes) {
            this.fileHash = fileHash;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.pieceSize = pieceSize;
            this.pieceCount = pieceCount;
            this.pieceHashes = pieceHashes;
        }
        
        public String getFileHash() { return fileHash; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public int getPieceSize() { return pieceSize; }
        public int getPieceCount() { return pieceCount; }
        public List<String> getPieceHashes() { return pieceHashes; }
    }
    
    /**
     * Represents a file piece
     */
    public static class Piece {
        private String fileHash;
        private int index;
        private byte[] data;
        private String hash;
        
        public Piece(String fileHash, int index, byte[] data, String hash) {
            this.fileHash = fileHash;
            this.index = index;
            this.data = data;
            this.hash = hash;
        }
        
        public String getFileHash() { return fileHash; }
        public int getIndex() { return index; }
        public byte[] getData() { return data; }
        public String getHash() { return hash; }
        
        public boolean verify() {
            return Hashing.verify(data, hash);
        }
    }
    
    /**
     * Transfer statistics
     */
    public static class TransferStats {
        private long bytesUploaded;
        private long bytesDownloaded;
        private int activeUploads;
        private int activeDownloads;
        private long startTime;
        
        public TransferStats() {
            this.startTime = System.currentTimeMillis();
        }
        
        public long getBytesUploaded() { return bytesUploaded; }
        public long getBytesDownloaded() { return bytesDownloaded; }
        public int getActiveUploads() { return activeUploads; }
        public int getActiveDownloads() { return activeDownloads; }
        public long getUptime() { return System.currentTimeMillis() - startTime; }
        
        public void addBytesUploaded(long bytes) { bytesUploaded += bytes; }
        public void addBytesDownloaded(long bytes) { bytesDownloaded += bytes; }
        public void incrementActiveUploads() { activeUploads++; }
        public void decrementActiveUploads() { activeUploads--; }
        public void incrementActiveDownloads() { activeDownloads++; }
        public void decrementActiveDownloads() { activeDownloads--; }
    }
}

