package org.torrentx.torrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for torrent file parsing
 */
public class TorrentFileTest {

    @Test
    public void testPieceInfoCreation() {
        byte[] hash = new byte[20];
        for (int i = 0; i < 20; i++) {
            hash[i] = (byte) i;
        }

        PieceInfo piece = new PieceInfo(0, hash);

        assertEquals(0, piece.getIndex());
        assertEquals("000102030405060708090a0b0c0d0e0f10111213", piece.getHashHex());
    }

    @Test
    public void testPieceVerification() {
        // Create test data
        String testData = "Hello, World!";
        byte[] data = testData.getBytes();

        // Calculate its hash
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data);

            PieceInfo piece = new PieceInfo(0, hash);

            // Verify it matches
            assertTrue(piece.verify(data));

            // Verify modified data doesn't match
            byte[] wrongData = "Hello, World!!".getBytes();
            assertFalse(piece.verify(wrongData));
        } catch (Exception e) {
            fail("SHA-1 not available");
        }
    }

    @Test
    public void testFileInfoCreation() {
        List<String> path = new ArrayList<>();
        path.add("folder");
        path.add("file.txt");

        FileInfo file = new FileInfo(path, 2048);

        assertEquals("folder/file.txt", file.getPathString());
        assertEquals(2048, file.getLength());
    }
}

