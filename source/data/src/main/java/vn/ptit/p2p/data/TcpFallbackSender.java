package vn.ptit.p2p.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Piece;
import vn.ptit.p2p.common.Models.Peer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP fallback for unreliable UDP connections
 */
public class TcpFallbackSender {
    private static final Logger logger = LoggerFactory.getLogger(TcpFallbackSender.class);
    private static final int CONNECTION_TIMEOUT = 5000;
    
    private final DataService dataService;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService executor;
    
    public TcpFallbackSender(DataService dataService) {
        this.dataService = dataService;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Start TCP server for accepting connections
     */
    public void startServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        
        Thread serverThread = new Thread(this::acceptLoop);
        serverThread.setDaemon(true);
        serverThread.start();
        
        logger.info("TCP fallback server started on port {}", port);
    }
    
    /**
     * Stop the TCP server
     */
    public void stopServer() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing TCP server socket", e);
            }
        }
        executor.shutdown();
        logger.info("TCP fallback server stopped");
    }
    
    /**
     * Send a piece to a peer via TCP
     */
    public CompletableFuture<Void> sendPiece(Peer peer, Piece piece) {
        return CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket(peer.getHost(), peer.getPort() + 1)) {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                
                // Write piece data
                dos.writeUTF("PIECE");
                dos.writeUTF(piece.getFileHash());
                dos.writeInt(piece.getIndex());
                dos.writeUTF(piece.getHash());
                dos.writeInt(piece.getData().length);
                dos.write(piece.getData());
                dos.flush();
                
                logger.debug("Sent piece {} via TCP to {}", piece.getIndex(), peer.getId());
                
            } catch (IOException e) {
                logger.error("Failed to send piece via TCP", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Request a piece from a peer via TCP
     */
    public CompletableFuture<Piece> requestPiece(Peer peer, String fileHash, int pieceIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try (Socket socket = new Socket(peer.getHost(), peer.getPort() + 1)) {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                
                // Send request
                dos.writeUTF("REQUEST");
                dos.writeUTF(fileHash);
                dos.writeInt(pieceIndex);
                dos.flush();
                
                // Receive response
                String magic = dis.readUTF();
                if (!"PIECE".equals(magic)) {
                    throw new IOException("Invalid response");
                }
                
                String respFileHash = dis.readUTF();
                int respIndex = dis.readInt();
                String hash = dis.readUTF();
                int length = dis.readInt();
                
                byte[] data = new byte[length];
                dis.readFully(data);
                
                Piece piece = new Piece(respFileHash, respIndex, data, hash);
                
                if (!piece.verify()) {
                    throw new IOException("Piece verification failed");
                }
                
                logger.debug("Received piece {} via TCP from {}", pieceIndex, peer.getId());
                return piece;
                
            } catch (IOException e) {
                logger.error("Failed to request piece via TCP", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Accept incoming TCP connections
     */
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting TCP connection", e);
                }
            }
        }
    }
    
    /**
     * Handle a client connection
     */
    private void handleClient(Socket socket) {
        try (socket) {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            
            String command = dis.readUTF();
            
            if ("REQUEST".equals(command)) {
                String fileHash = dis.readUTF();
                int pieceIndex = dis.readInt();
                
                // Get piece from data service
                Peer requestingPeer = new Peer("unknown", "unknown", 
                                              socket.getInetAddress().getHostAddress(), 
                                              socket.getPort());
                Piece piece = dataService.handleIncomingRequest(requestingPeer, fileHash, pieceIndex);
                
                if (piece != null) {
                    dos.writeUTF("PIECE");
                    dos.writeUTF(piece.getFileHash());
                    dos.writeInt(piece.getIndex());
                    dos.writeUTF(piece.getHash());
                    dos.writeInt(piece.getData().length);
                    dos.write(piece.getData());
                    dos.flush();
                } else {
                    dos.writeUTF("ERROR");
                    dos.writeUTF("Piece not available");
                    dos.flush();
                }
            }
        } catch (IOException e) {
            logger.error("Error handling TCP client", e);
        }
    }
}

