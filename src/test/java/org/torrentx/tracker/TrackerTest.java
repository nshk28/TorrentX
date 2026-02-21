package org.torrentx.tracker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tracker Communication Module
 */
public class TrackerTest {

    @Test
    public void testPeerAddressCreation() {
        PeerAddress peer = new PeerAddress("192.168.1.1", 6881);

        assertEquals("192.168.1.1", peer.getIp());
        assertEquals(6881, peer.getPort());
        assertEquals("192.168.1.1:6881", peer.getAddress());
    }

    @Test
    public void testPeerAddressInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PeerAddress("192.168.1.1", 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PeerAddress("192.168.1.1", 70000);
        });
    }

    @Test
    public void testPeerAddressEquality() {
        PeerAddress peer1 = new PeerAddress("192.168.1.1", 6881);
        PeerAddress peer2 = new PeerAddress("192.168.1.1", 6881);
        PeerAddress peer3 = new PeerAddress("192.168.1.2", 6881);

        assertEquals(peer1, peer2);
        assertNotEquals(peer1, peer3);
    }

    @Test
    public void testPeerIdGeneration() {
        byte[] peerId1 = PeerId.generate();
        byte[] peerId2 = PeerId.generate();

        // Should be 20 bytes
        assertEquals(20, peerId1.length);
        assertEquals(20, peerId2.length);

        // Should be different (extremely high probability)
        assertNotEquals(PeerId.toHex(peerId1), PeerId.toHex(peerId2));
    }

    @Test
    public void testPeerIdValidation() {
        byte[] validId = PeerId.generate();
        byte[] invalidId = new byte[19];

        assertTrue(PeerId.isValid(validId));
        assertFalse(PeerId.isValid(invalidId));
        assertFalse(PeerId.isValid(null));
    }

    @Test
    public void testPeerIdHex() {
        byte[] peerId = new byte[20];
        for (int i = 0; i < 20; i++) {
            peerId[i] = (byte) i;
        }

        String hex = PeerId.toHex(peerId);
        assertEquals(40, hex.length()); // 20 bytes = 40 hex characters
        assertTrue(hex.matches("[0-9a-f]{40}"));
    }
}

