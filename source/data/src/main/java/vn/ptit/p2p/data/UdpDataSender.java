package vn.ptit.p2p.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Piece;
import vn.ptit.p2p.common.Models.Peer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Sends data pieces via UDP protocol
 */
public class UdpDataSender {
    private static final Logger logger = LoggerFactory.getLogger(UdpDataSender.class);
    private static final int MAX_PACKET_SIZE = 65507; // Maximum UDP packet size
    private static final int HEADER_SIZE = 100; // Reserve space for header
    
    private final DataService dataService;
    private DatagramSocket socket;
    
    public UdpDataSender(DataService dataService) {
        this.dataService = dataService;
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(5000); // 5 second timeout
        } catch (IOException e) {
            logger.error("Failed to create UDP socket", e);
        }
    }
    
    /**
     * Send a piece to a peer via UDP
     */
    public CompletableFuture<Void> sendPiece(Peer peer, Piece piece) {
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] data = serializePiece(piece);
                
                if (data.length > MAX_PACKET_SIZE) {
                    throw new IOException("Piece too large for UDP: " + data.length);
                }
                
                InetAddress address = InetAddress.getByName(peer.getHost());
                DatagramPacket packet = new DatagramPacket(data, data.length, address, peer.getPort());
                
                socket.send(packet);
                logger.debug("Sent piece {} via UDP to {}", piece.getIndex(), peer.getId());
                
            } catch (IOException e) {
                logger.error("Failed to send piece via UDP", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Serialize a piece into bytes for transmission
     */
    private byte[] serializePiece(Piece piece) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // Write header
        dos.writeUTF("PIECE");
        dos.writeUTF(piece.getFileHash());
        dos.writeInt(piece.getIndex());
        dos.writeUTF(piece.getHash());
        
        // Write data
        dos.writeInt(piece.getData().length);
        dos.write(piece.getData());
        
        dos.flush();
        return baos.toByteArray();
    }
    
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}

