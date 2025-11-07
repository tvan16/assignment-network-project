package vn.ptit.p2p.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.ptit.p2p.common.Models.Peer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for maintaining list of active peers
 */
public class PeerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PeerRegistry.class);
    
    private final Map<String, Peer> peers;
    private final List<PeerRegistryListener> listeners;
    
    public PeerRegistry() {
        this.peers = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }
    
    /**
     * Add or update a peer in the registry
     */
    public void addPeer(Peer peer) {
        Peer existing = peers.get(peer.getId());
        
        if (existing == null) {
            peers.put(peer.getId(), peer);
            logger.info("Added peer: {} ({}:{})", peer.getId(), peer.getHost(), peer.getPort());
            notifyPeerAdded(peer);
        } else {
            existing.setLastSeen(System.currentTimeMillis());
            logger.debug("Updated peer: {}", peer.getId());
        }
    }
    
    /**
     * Remove a peer from the registry
     */
    public void removePeer(String peerId) {
        Peer removed = peers.remove(peerId);
        if (removed != null) {
            logger.info("Removed peer: {}", peerId);
            notifyPeerRemoved(removed);
        }
    }
    
    /**
     * Get a peer by ID
     */
    public Peer getPeer(String peerId) {
        return peers.get(peerId);
    }
    
    /**
     * Get all active peers
     */
    public Collection<Peer> getAllPeers() {
        return new ArrayList<>(peers.values());
    }
    
    /**
     * Get count of active peers
     */
    public int getPeerCount() {
        return peers.size();
    }
    
    /**
     * Check if a peer exists
     */
    public boolean hasPeer(String peerId) {
        return peers.containsKey(peerId);
    }
    
    /**
     * Add a listener for peer registry events
     */
    public void addListener(PeerRegistryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(PeerRegistryListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyPeerAdded(Peer peer) {
        for (PeerRegistryListener listener : listeners) {
            try {
                listener.onPeerAdded(peer);
            } catch (Exception e) {
                logger.error("Error notifying listener of peer added", e);
            }
        }
    }
    
    private void notifyPeerRemoved(Peer peer) {
        for (PeerRegistryListener listener : listeners) {
            try {
                listener.onPeerRemoved(peer);
            } catch (Exception e) {
                logger.error("Error notifying listener of peer removed", e);
            }
        }
    }
    
    /**
     * Interface for listening to peer registry events
     */
    public interface PeerRegistryListener {
        void onPeerAdded(Peer peer);
        void onPeerRemoved(Peer peer);
    }
}

