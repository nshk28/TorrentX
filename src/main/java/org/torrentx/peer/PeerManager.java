package org.torrentx.peer;

import org.torrentx.tracker.PeerAddress;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Peer Manager
 *
 * Manages a pool of peer connections.
 * Handles peer discovery, connection management, and message routing.
 */
public class PeerManager {

    private final Map<String, PeerConnection> connections;
    private final Queue<PeerAddress> pendingPeers;
    private final byte[] infoHash;
    private final byte[] peerId;

    private static final int MAX_CONNECTIONS = 4;
    private static final int CONNECT_TIMEOUT = 30000;

    /**
     * Create peer manager
     */
    public PeerManager(byte[] infoHash, byte[] peerId) {
        if (infoHash.length != 20 || peerId.length != 20) {
            throw new IllegalArgumentException("Hashes must be 20 bytes");
        }

        this.connections = new ConcurrentHashMap<>();
        this.pendingPeers = new ConcurrentLinkedQueue<>();
        this.infoHash = infoHash.clone();
        this.peerId = peerId.clone();
    }

    /**
     * Add peers to connection queue
     */
    public void addPeers(List<PeerAddress> peers) {
        for (PeerAddress peer : peers) {
            if (!connections.containsKey(peer.getAddress())) {
                pendingPeers.offer(peer);
            }
        }
    }

    /**
     * Connect to next available peer
     */
    public boolean connectToPeer() {
        if (connections.size() >= MAX_CONNECTIONS) {
            return false; // Already at max connections
        }

        PeerAddress peer = pendingPeers.poll();
        if (peer == null) {
            return false; // No more peers to try
        }

        PeerConnection conn = new PeerConnection(peer, infoHash, peerId);

        if (conn.connect() && conn.handshake()) {
            connections.put(peer.getAddress(), conn);
            return true;
        } else {
            conn.close();
            return false;
        }
    }

    /**
     * Connect to multiple peers concurrently
     */
    public int connectToPeers(int count) {
        int connected = 0;
        for (int i = 0; i < count && connections.size() < MAX_CONNECTIONS; i++) {
            if (connectToPeer()) {
                connected++;
            } else {
                break; // No more peers available
            }
        }
        return connected;
    }

    /**
     * Get active connections
     */
    public List<PeerConnection> getConnections() {
        return new ArrayList<>(connections.values());
    }

    /**
     * Get connection to specific peer
     */
    public PeerConnection getConnection(String peerAddress) {
        return connections.get(peerAddress);
    }

    /**
     * Get number of active connections
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Get number of pending peers
     */
    public int getPendingCount() {
        return pendingPeers.size();
    }

    /**
     * Remove closed connections
     */
    public void cleanupClosedConnections() {
        connections.values().removeIf(conn -> !conn.isConnected());
    }

    /**
     * Close all connections
     */
    public void closeAll() {
        for (PeerConnection conn : connections.values()) {
            conn.close();
        }
        connections.clear();
    }

    /**
     * Send interested to all connected peers
     */
    public void sendInterestedToAll() throws IOException {
        for (PeerConnection conn : connections.values()) {
            try {
                conn.sendInterested();
            } catch (IOException e) {
                System.err.println("Failed to send interested to " + conn.getPeerAddress());
            }
        }
    }

    /**
     * Request block from a peer
     */
    public void requestBlock(String peerAddress, int index, int begin, int length)
            throws IOException {
        PeerConnection conn = connections.get(peerAddress);
        if (conn == null) {
            throw new IOException("No connection to " + peerAddress);
        }

        if (!conn.isHandshakeComplete()) {
            throw new IOException("Handshake not complete");
        }

        conn.requestBlock(index, begin, length);
    }

    /**
     * Receive message from any connected peer
     * Returns null if all connections fail
     */
    public PeerMessage receiveMessageFromAny() throws IOException {
        for (PeerConnection conn : connections.values()) {
            try {
                PeerMessage msg = conn.receiveMessage();
                if (msg != null) {
                    return msg;
                }
            } catch (IOException e) {
                // Try next peer
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "PeerManager{" +
                "connections=" + connections.size() +
                ", pending=" + pendingPeers.size() +
                ", maxConnections=" + MAX_CONNECTIONS +
                '}';
    }
}

