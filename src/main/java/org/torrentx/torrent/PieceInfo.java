package org.torrentx.torrent;

import java.util.Arrays;

/**
 * Represents a single piece in a torrent
 * Contains piece index and SHA-1 hash for verification
 */
public class PieceInfo {
    private final int index;
    private final byte[] hash; // 20-byte SHA-1 hash

    public PieceInfo(int index, byte[] hash) {
        if (hash.length != 20) {
            throw new IllegalArgumentException("Hash must be exactly 20 bytes");
        }
        this.index = index;
        this.hash = Arrays.copyOf(hash, 20);
    }

    public int getIndex() {
        return index;
    }

    public byte[] getHash() {
        return Arrays.copyOf(hash, 20);
    }

    /**
     * Get hash as hexadecimal string
     */
    public String getHashHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verify if given data matches this piece's hash
     */
    public boolean verify(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] calculated = digest.digest(data);
            return Arrays.equals(calculated, hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    @Override
    public String toString() {
        return "PieceInfo{" +
                "index=" + index +
                ", hash=" + getHashHex() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PieceInfo pieceInfo = (PieceInfo) o;
        return index == pieceInfo.index && Arrays.equals(hash, pieceInfo.hash);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(index);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }
}

