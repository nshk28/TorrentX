package org.torrentx.peer;

import org.junit.jupiter.api.Test;
import org.torrentx.tracker.PeerAddress;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Peer Connection Module
 */
public class PeerTest {

    @Test
    public void testPeerAddressConnection() {
        PeerAddress peer = new PeerAddress("192.168.1.1", 6881);
        byte[] infoHash = new byte[20];
        byte[] peerId = new byte[20];

        PeerConnection conn = new PeerConnection(peer, infoHash, peerId);

        assertEquals(peer, conn.getPeerAddress());
        assertFalse(conn.isConnected());
        assertFalse(conn.isHandshakeComplete());
    }

    @Test
    public void testPeerMessageCreation() {
        byte[] payload = new byte[]{1, 2, 3, 4};
        PeerMessage msg = new PeerMessage(PeerMessage.HAVE, payload);

        assertEquals(PeerMessage.HAVE, msg.getId());
        assertArrayEquals(payload, msg.getPayload());
        assertEquals("have", msg.getMessageName());
    }

    @Test
    public void testKeepAliveMessage() {
        PeerMessage msg = new PeerMessage(PeerMessage.KEEP_ALIVE, new byte[0]);

        assertEquals(PeerMessage.KEEP_ALIVE, msg.getId());
        assertEquals("keep-alive", msg.getMessageName());
    }

    @Test
    public void testRequestMessage() {
        byte[] payload = new byte[12];
        // Index: 5
        payload[3] = 5;
        // Begin: 0
        // Length: 16384 (0x4000)
        payload[10] = 0x40;

        PeerMessage msg = new PeerMessage(PeerMessage.REQUEST, payload);
        PeerMessage.RequestData req = msg.parseRequest();

        assertEquals(5, req.index);
        assertEquals(0, req.begin);
        assertEquals(0x4000, req.length);
    }

    @Test
    public void testPeerManagerCreation() {
        byte[] infoHash = new byte[20];
        byte[] peerId = new byte[20];

        PeerManager manager = new PeerManager(infoHash, peerId);

        assertEquals(0, manager.getConnectionCount());
        assertEquals(0, manager.getPendingCount());
    }

    @Test
    public void testPeerManagerAddPeers() {
        byte[] infoHash = new byte[20];
        byte[] peerId = new byte[20];

        PeerManager manager = new PeerManager(infoHash, peerId);

        List<PeerAddress> peers = new ArrayList<>();
        peers.add(new PeerAddress("192.168.1.1", 6881));
        peers.add(new PeerAddress("192.168.1.2", 6882));
        peers.add(new PeerAddress("192.168.1.3", 6883));

        manager.addPeers(peers);

        assertEquals(3, manager.getPendingCount());
    }

    @Test
    public void testInvalidHashLength() {
        byte[] shortHash = new byte[10];
        byte[] peerId = new byte[20];

        assertThrows(IllegalArgumentException.class, () -> {
            new PeerManager(shortHash, peerId);
        });
    }

    @Test
    public void testMessageNameMapping() {
        assertEquals("choke", new PeerMessage(PeerMessage.CHOKE, new byte[0]).getMessageName());
        assertEquals("unchoke", new PeerMessage(PeerMessage.UNCHOKE, new byte[0]).getMessageName());
        assertEquals("interested", new PeerMessage(PeerMessage.INTERESTED, new byte[0]).getMessageName());
        assertEquals("not interested", new PeerMessage(PeerMessage.NOT_INTERESTED, new byte[0]).getMessageName());
        assertEquals("bitfield", new PeerMessage(PeerMessage.BITFIELD, new byte[0]).getMessageName());
        assertEquals("piece", new PeerMessage(PeerMessage.PIECE, new byte[0]).getMessageName());
        assertEquals("cancel", new PeerMessage(PeerMessage.CANCEL, new byte[0]).getMessageName());
    }
}

