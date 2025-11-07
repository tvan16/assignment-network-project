package vn.ptit.p2p.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Json;
import vn.ptit.p2p.common.Models.Peer;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for discovering peers in the network using multicast
 */
public class DiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);
    
    private final PeerRegistry peerRegistry;
    private final String peerId;
    private final int port;
    private final String multicastAddress;
    private final int announceInterval;
    private final int peerTimeout;
    
    private MulticastSocket socket;
    private InetAddress group;
    private ScheduledExecutorService scheduler;
    private volatile boolean running;
    
    public DiscoveryService(PeerRegistry peerRegistry, String peerId, int port,
                           String multicastAddress, int announceInterval, int peerTimeout) {
        this.peerRegistry = peerRegistry;
        this.peerId = peerId;
        this.port = port;
        this.multicastAddress = multicastAddress;
        this.announceInterval = announceInterval;
        this.peerTimeout = peerTimeout;
    }
    
    /**
     * Start the discovery service
     */
    public void start() throws IOException {
        logger.info("Starting discovery service on {}:{}", multicastAddress, port);
        
        socket = new MulticastSocket(port);
        group = InetAddress.getByName(multicastAddress);
        socket.joinGroup(group);
        
        running = true;
        
        // Start listener thread
        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
        
        // Start announcer
        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::announce, 0, announceInterval, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupExpiredPeers, peerTimeout, peerTimeout, TimeUnit.SECONDS);
        
        logger.info("Discovery service started");
    }
    
    /**
     * Stop the discovery service
     */
    public void stop() {
        logger.info("Stopping discovery service");
        running = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (socket != null) {
            try {
                socket.leaveGroup(group);
            } catch (IOException e) {
                logger.error("Error leaving multicast group", e);
            }
            socket.close();
        }
        
        logger.info("Discovery service stopped");
    }
    
    /**
     * Announce this peer to the network
     */
    private void announce() {
        try {
            Map<String, Object> announcement = new HashMap<>();
            announcement.put("type", "announce");
            announcement.put("peer_id", peerId);
            announcement.put("timestamp", System.currentTimeMillis());
            
            String message = Json.toJson(announcement);
            byte[] data = message.getBytes();
            
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);
            
            logger.debug("Announced presence to network");
        } catch (IOException e) {
            logger.error("Error announcing to network", e);
        }
    }
    
    /**
     * Listen for peer announcements
     */
    private void listen() {
        byte[] buffer = new byte[1024];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                handleMessage(message, packet.getAddress());
            } catch (IOException e) {
                if (running) {
                    logger.error("Error receiving discovery message", e);
                }
            }
        }
    }
    
    /**
     * Handle received discovery message
     */
    @SuppressWarnings("unchecked")
    private void handleMessage(String message, InetAddress address) {
        try {
            Map<String, Object> data = Json.fromJson(message, Map.class);
            String type = (String) data.get("type");
            
            if ("announce".equals(type)) {
                String remotePeerId = (String) data.get("peer_id");
                
                // Don't add ourselves
                if (!peerId.equals(remotePeerId)) {
                    Peer peer = new Peer(remotePeerId, remotePeerId, 
                                       address.getHostAddress(), port);
                    peerRegistry.addPeer(peer);
                    logger.debug("Discovered peer: {}", remotePeerId);
                }
            }
        } catch (Exception e) {
            logger.debug("Error handling discovery message", e);
        }
    }
    
    /**
     * Remove peers that haven't been seen recently
     */
    private void cleanupExpiredPeers() {
        long now = System.currentTimeMillis();
        long timeoutMillis = peerTimeout * 1000L;
        
        peerRegistry.getAllPeers().stream()
            .filter(peer -> (now - peer.getLastSeen()) > timeoutMillis)
            .forEach(peer -> {
                peerRegistry.removePeer(peer.getId());
                logger.info("Peer {} timed out", peer.getId());
            });
    }
}

