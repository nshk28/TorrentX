package org.torrentx.tracker;

import java.util.Random;

/**
 * Generates unique Peer IDs for BitTorrent
 *
 * Peer ID is a 20-byte identifier used to uniquely identify a peer in the network.
 * Format: "-TorrentX-" (9 bytes) + version (3 bytes) + random (8 bytes)
 *
 * Standard format: <client-id><version><random>
 * Example: "-TX1000-"XXXXXXXXXXXXXXXX (20 bytes total)
 */
public class PeerId {

    private static final String CLIENT_ID = "TorrentX";
    private static final String VERSION = "010";  // Version 0.1.0
    private static final Random random = new Random();

    /**
     * Generate a random 20-byte peer ID
     */
    public static byte[] generate() {
        byte[] peerId = new byte[20];

        // Client ID: "-TX-" (4 bytes)
        peerId[0] = '-';
        peerId[1] = 'T';
        peerId[2] = 'X';
        peerId[3] = '1';

        // Version: "010" (3 bytes)
        peerId[4] = '0';
        peerId[5] = '1';
        peerId[6] = '0';

        // Random bytes (13 bytes)
        for (int i = 7; i < 20; i++) {
            peerId[i] = (byte) (random.nextInt(256) - 128);
        }

        return peerId;
    }

    /**
     * Get peer ID as hexadecimal string
     */
    public static String toHex(byte[] peerId) {
        StringBuilder hex = new StringBuilder();
        for (byte b : peerId) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Validate peer ID (must be 20 bytes)
     */
    public static boolean isValid(byte[] peerId) {
        return peerId != null && peerId.length == 20;
    }
}

