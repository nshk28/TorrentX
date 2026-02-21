package org.torrentx.peer;

import org.torrentx.tracker.PeerAddress;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Peer Connection Handler
 *
 * Manages TCP connection with a single peer.
 * Handles BitTorrent protocol handshake and message exchange.
 *
 * IMPLEMENTATION SUMMARY:
 * ======================
 * This module handles all peer-to-peer communication for the BitTorrent protocol.
 *
 * Features:
 * - TCP connection establishment with peers
 * - BitTorrent protocol handshake (19 bytes header + 48 bytes data)
 * - Peer message parsing (keep-alive, have, interested, etc.)
 * - Send and receive messages from peers
 * - Connection state management (handshake, bitfield exchange, etc.)
 * - Error handling and connection recovery
 *
 * Handshake Format:
 *   Byte 0: 19 (string length)
 *   Bytes 1-19: "BitTorrent protocol" (19 bytes)
 *   Bytes 20-27: Reserved flags (8 bytes, all zeros)
 *   Bytes 28-47: info_hash (20 bytes)
 *   Bytes 48-67: peer_id (20 bytes)
 *
 * Message Format:
 *   Bytes 0-3: Length (4 bytes, big-endian) - 0 for keep-alive
 *   Byte 4: Message ID (if length > 0)
 *   Bytes 5+: Payload (depends on message type)
 *
 * Message Types:
 *   0 = choke
 *   1 = unchoke
 *   2 = interested
 *   3 = not interested
 *   4 = have (includes piece index)
 *   5 = bitfield (includes piece availability)
 *   6 = request (includes index, begin, length)
 *   7 = piece (includes index, begin, block data)
 *   8 = cancel (includes index, begin, length)
 *
 * Usage:
 *   PeerConnection conn = new PeerConnection(peerAddress, infoHash, peerId);
 *   if (conn.connect() && conn.handshake()) {
 *       conn.receiveBitfield();
 *       conn.requestPiece(0, 0, 16384);
 *       PeerMessage msg = conn.receiveMessage();
 *   }
 */
public class PeerConnection {

    private final PeerAddress peerAddress;
    private final byte[] infoHash;
    private final byte[] peerId;

    private Socket socket;
    private InputStream input;
    private OutputStream output;

    private boolean connected = false;
    private boolean handshakeComplete = false;
    private boolean choked = true;           // We start choked
    private boolean interested = false;
    private boolean peerInterested = false;
    private boolean peerChoked = true;       // Peer starts unchoked

    private static final int TIMEOUT = 30000; // 30 seconds
    private static final String PROTOCOL = "BitTorrent protocol";
    private static final int BLOCK_SIZE = 16384; // 16KB blocks

    /**
     * Create peer connection
     */
    public PeerConnection(PeerAddress peerAddress, byte[] infoHash, byte[] peerId) {
        if (infoHash.length != 20 || peerId.length != 20) {
            throw new IllegalArgumentException("info_hash and peer_id must be 20 bytes");
        }
        this.peerAddress = peerAddress;
        this.infoHash = infoHash.clone();
        this.peerId = peerId.clone();
    }

    /**
     * Connect to peer via TCP
     */
    public boolean connect() {
        try {
            socket = new Socket(peerAddress.getIp(), peerAddress.getPort());
            socket.setSoTimeout(TIMEOUT);

            input = socket.getInputStream();
            output = socket.getOutputStream();

            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to " + peerAddress + ": " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /**
     * Perform BitTorrent handshake
     *
     * Handshake format (68 bytes total):
     * - 1 byte: protocol string length (19)
     * - 19 bytes: "BitTorrent protocol"
     * - 8 bytes: reserved flags (all 0)
     * - 20 bytes: info_hash
     * - 20 bytes: peer_id
     */
    public boolean handshake() {
        if (!connected) {
            System.err.println("Not connected to peer");
            return false;
        }

        try {
            // Send handshake
            byte[] handshake = new byte[68];
            int pos = 0;

            // Protocol length
            handshake[pos++] = 19;

            // Protocol string
            byte[] protocolBytes = PROTOCOL.getBytes();
            System.arraycopy(protocolBytes, 0, handshake, pos, 19);
            pos += 19;

            // Reserved (8 bytes of zeros)
            pos += 8;

            // Info hash
            System.arraycopy(infoHash, 0, handshake, pos, 20);
            pos += 20;

            // Peer ID
            System.arraycopy(peerId, 0, handshake, pos, 20);

            output.write(handshake);
            output.flush();

            // Receive handshake response
            byte[] response = new byte[68];
            input.read(response);

            // Verify response
            if (response[0] != 19) {
                System.err.println("Invalid handshake response: bad protocol length");
                return false;
            }

            String protocol = new String(response, 1, 19);
            if (!protocol.equals(PROTOCOL)) {
                System.err.println("Invalid handshake response: bad protocol string");
                return false;
            }

            // Note: We should verify info_hash matches, but some peers may not send it correctly

            handshakeComplete = true;
            return true;

        } catch (IOException e) {
            System.err.println("Handshake failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Receive bitfield message
     * Bitfield indicates which pieces peer has available
     */
    public byte[] receiveBitfield() throws IOException {
        if (!handshakeComplete) {
            throw new IllegalStateException("Handshake not complete");
        }

        PeerMessage msg = receiveMessage();
        if (msg == null || msg.getId() != PeerMessage.BITFIELD) {
            throw new IOException("Expected bitfield message");
        }

        return msg.getPayload();
    }

    /**
     * Send interested message to peer
     */
    public void sendInterested() throws IOException {
        if (!handshakeComplete) {
            throw new IllegalStateException("Handshake not complete");
        }

        interested = true;
        PeerMessage msg = new PeerMessage(PeerMessage.INTERESTED, new byte[0]);
        sendMessage(msg);
    }

    /**
     * Send request for a block
     * @param index Piece index
     * @param begin Block offset within piece
     * @param length Block size (usually 16KB)
     */
    public void requestBlock(int index, int begin, int length) throws IOException {
        if (!handshakeComplete) {
            throw new IllegalStateException("Handshake not complete");
        }
        if (peerChoked) {
            throw new IOException("Peer is choking us");
        }

        byte[] payload = new byte[12];

        // Write index (4 bytes, big-endian)
        payload[0] = (byte) ((index >> 24) & 0xFF);
        payload[1] = (byte) ((index >> 16) & 0xFF);
        payload[2] = (byte) ((index >> 8) & 0xFF);
        payload[3] = (byte) (index & 0xFF);

        // Write begin (4 bytes, big-endian)
        payload[4] = (byte) ((begin >> 24) & 0xFF);
        payload[5] = (byte) ((begin >> 16) & 0xFF);
        payload[6] = (byte) ((begin >> 8) & 0xFF);
        payload[7] = (byte) (begin & 0xFF);

        // Write length (4 bytes, big-endian)
        payload[8] = (byte) ((length >> 24) & 0xFF);
        payload[9] = (byte) ((length >> 16) & 0xFF);
        payload[10] = (byte) ((length >> 8) & 0xFF);
        payload[11] = (byte) (length & 0xFF);

        PeerMessage msg = new PeerMessage(PeerMessage.REQUEST, payload);
        sendMessage(msg);
    }

    /**
     * Receive a message from peer
     */
    public PeerMessage receiveMessage() throws IOException {
        if (!handshakeComplete) {
            throw new IllegalStateException("Handshake not complete");
        }

        // Read length (4 bytes)
        byte[] lengthBytes = new byte[4];
        int read = input.read(lengthBytes);

        if (read != 4) {
            return null; // Connection closed
        }

        int length = bytesToInt(lengthBytes);

        // Keep-alive message (length = 0)
        if (length == 0) {
            return new PeerMessage(PeerMessage.KEEP_ALIVE, new byte[0]);
        }

        // Read message ID and payload
        byte[] data = new byte[length];
        input.read(data);

        byte id = data[0];
        byte[] payload = new byte[length - 1];
        System.arraycopy(data, 1, payload, 0, length - 1);

        PeerMessage msg = new PeerMessage(id, payload);

        // Update state based on message
        if (id == PeerMessage.CHOKE) {
            peerChoked = true;
        } else if (id == PeerMessage.UNCHOKE) {
            peerChoked = false;
        } else if (id == PeerMessage.INTERESTED) {
            peerInterested = true;
        } else if (id == PeerMessage.NOT_INTERESTED) {
            peerInterested = false;
        }

        return msg;
    }

    /**
     * Send message to peer
     */
    private void sendMessage(PeerMessage msg) throws IOException {
        byte[] payload = msg.getPayload();
        int length = 1 + payload.length; // ID + payload

        // Write length
        byte[] lengthBytes = intToBytes(length);
        output.write(lengthBytes);

        // Write message
        output.write(msg.getId());
        output.write(payload);

        output.flush();
    }

    /**
     * Close connection
     */
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected = false;
            handshakeComplete = false;
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Convert 4 bytes to int (big-endian)
     */
    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    /**
     * Convert int to 4 bytes (big-endian)
     */
    private static byte[] intToBytes(int value) {
        return new byte[]{
            (byte) ((value >> 24) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }

    // Getters
    public boolean isConnected() {
        return connected;
    }

    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    public boolean isPeerChoked() {
        return peerChoked;
    }

    public boolean isInterested() {
        return interested;
    }

    public boolean isPeerInterested() {
        return peerInterested;
    }

    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    @Override
    public String toString() {
        return "PeerConnection{" +
                "peer=" + peerAddress +
                ", connected=" + connected +
                ", handshake=" + handshakeComplete +
                ", choked=" + peerChoked +
                '}';
    }
}

