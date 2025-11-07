package vn.ptit.p2p.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Piece;
import vn.ptit.p2p.common.Models.Peer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives data pieces via UDP protocol
 */
public class UdpDataReceiver {
    private static final Logger logger = LoggerFactory.getLogger(UdpDataReceiver.class);
    private static final int MAX_PACKET_SIZE = 65507;
    
    private final DataService dataService;
    private DatagramSocket socket;
    private volatile boolean running;
    private Thread receiverThread;
    
    private final ConcurrentHashMap<String, CompletableFuture<Piece>> pendingRequests = new ConcurrentHashMap<>();
    
    public UdpDataReceiver(DataService dataService) {
        this.dataService = dataService;
    }
    
    /**
     * Start listening for incoming UDP packets
     */
    public void start(int port) throws IOException {
        socket = new DatagramSocket(port);
        running = true;
        
        receiverThread = new Thread(this::receiveLoop);
        receiverThread.setDaemon(true);
        receiverThread.start();
        
        logger.info("UDP receiver started on port {}", port);
    }
    
    /**
     * Stop the UDP receiver
     */
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        logger.info("UDP receiver stopped");
    }
    
    /**
     * Request a piece from a peer
     */
    public CompletableFuture<Piece> requestPiece(Peer peer, String fileHash, int pieceIndex) {
        String key = fileHash + ":" + pieceIndex;
        CompletableFuture<Piece> future = new CompletableFuture<>();
        pendingRequests.put(key, future);
        
        // Send request (simplified - in real implementation would send actual request)
        logger.debug("Requesting piece {} of file {} via UDP", pieceIndex, fileHash);
        
        return future;
    }
    
    /**
     * Main receive loop
     */
    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                handlePacket(packet);
            } catch (IOException e) {
                if (running) {
                    logger.error("Error receiving UDP packet", e);
                }
            }
        }
    }
    
    /**
     * Handle received UDP packet
     */
    private void handlePacket(DatagramPacket packet) {
        try {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
            
            Piece piece = deserializePiece(data);
            
            if (piece != null) {
                String key = piece.getFileHash() + ":" + piece.getIndex();
                CompletableFuture<Piece> future = pendingRequests.remove(key);
                
                if (future != null) {
                    // Verify piece integrity
                    if (piece.verify()) {
                        future.complete(piece);
                        logger.debug("Received and verified piece {}", piece.getIndex());
                    } else {
                        future.completeExceptionally(new IOException("Piece verification failed"));
                        logger.error("Piece {} failed verification", piece.getIndex());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling UDP packet", e);
        }
    }
    
    /**
     * Deserialize a piece from received bytes
     */
    private Piece deserializePiece(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        
        // Read header
        String magic = dis.readUTF();
        if (!"PIECE".equals(magic)) {
            return null;
        }
        
        String fileHash = dis.readUTF();
        int index = dis.readInt();
        String hash = dis.readUTF();
        
        // Read data
        int length = dis.readInt();
        byte[] pieceData = new byte[length];
        dis.readFully(pieceData);
        
        return new Piece(fileHash, index, pieceData, hash);
    }
}

