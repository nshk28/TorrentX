package org.torrentx.peer;

/**
 * BitTorrent Peer Message
 *
 * Represents a single message in the peer-to-peer protocol.
 *
 * Message Types:
 *   -1 = Keep-alive (length = 0, no ID)
 *    0 = choke
 *    1 = unchoke
 *    2 = interested
 *    3 = not interested
 *    4 = have (payload: piece index)
 *    5 = bitfield (payload: piece availability bits)
 *    6 = request (payload: index, begin, length)
 *    7 = piece (payload: index, begin, block data)
 *    8 = cancel (payload: index, begin, length)
 */
public class PeerMessage {

    // Message IDs
    public static final byte KEEP_ALIVE = -1;
    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOT_INTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;
    public static final byte CANCEL = 8;

    private final byte id;
    private final byte[] payload;

    /**
     * Create a peer message
     */
    public PeerMessage(byte id, byte[] payload) {
        this.id = id;
        this.payload = payload != null ? payload.clone() : new byte[0];
    }

    /**
     * Get message ID
     */
    public byte getId() {
        return id;
    }

    /**
     * Get payload (defensive copy)
     */
    public byte[] getPayload() {
        return payload.clone();
    }

    /**
     * Get message name for logging
     */
    public String getMessageName() {
        switch (id) {
            case KEEP_ALIVE: return "keep-alive";
            case CHOKE: return "choke";
            case UNCHOKE: return "unchoke";
            case INTERESTED: return "interested";
            case NOT_INTERESTED: return "not interested";
            case HAVE: return "have";
            case BITFIELD: return "bitfield";
            case REQUEST: return "request";
            case PIECE: return "piece";
            case CANCEL: return "cancel";
            default: return "unknown";
        }
    }

    /**
     * Parse piece index from have message
     */
    public int getHavePieceIndex() {
        if (id != HAVE || payload.length != 4) {
            throw new IllegalStateException("Not a have message");
        }

        return ((payload[0] & 0xFF) << 24) |
               ((payload[1] & 0xFF) << 16) |
               ((payload[2] & 0xFF) << 8) |
               (payload[3] & 0xFF);
    }

    /**
     * Parse request data
     */
    public RequestData parseRequest() {
        if (id != REQUEST || payload.length != 12) {
            throw new IllegalStateException("Not a request message");
        }

        int index = bytesToInt(0);
        int begin = bytesToInt(4);
        int length = bytesToInt(8);

        return new RequestData(index, begin, length);
    }

    /**
     * Parse piece data
     */
    public PieceData parsePiece() {
        if (id != PIECE || payload.length < 8) {
            throw new IllegalStateException("Not a piece message");
        }

        int index = bytesToInt(0);
        int begin = bytesToInt(4);
        byte[] block = new byte[payload.length - 8];
        System.arraycopy(payload, 8, block, 0, block.length);

        return new PieceData(index, begin, block);
    }

    /**
     * Convert 4 bytes starting at offset to int
     */
    private int bytesToInt(int offset) {
        return ((payload[offset] & 0xFF) << 24) |
               ((payload[offset + 1] & 0xFF) << 16) |
               ((payload[offset + 2] & 0xFF) << 8) |
               (payload[offset + 3] & 0xFF);
    }

    @Override
    public String toString() {
        return "PeerMessage{" +
                "id=" + id +
                ", name='" + getMessageName() + '\'' +
                ", payloadSize=" + payload.length +
                '}';
    }

    /**
     * Request message data
     */
    public static class RequestData {
        public final int index;
        public final int begin;
        public final int length;

        public RequestData(int index, int begin, int length) {
            this.index = index;
            this.begin = begin;
            this.length = length;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "piece=" + index +
                    ", offset=" + begin +
                    ", size=" + length +
                    '}';
        }
    }

    /**
     * Piece message data
     */
    public static class PieceData {
        public final int index;
        public final int begin;
        public final byte[] block;

        public PieceData(int index, int begin, byte[] block) {
            this.index = index;
            this.begin = begin;
            this.block = block.clone();
        }

        @Override
        public String toString() {
            return "Piece{" +
                    "piece=" + index +
                    ", offset=" + begin +
                    ", blockSize=" + block.length +
                    '}';
        }
    }
}


