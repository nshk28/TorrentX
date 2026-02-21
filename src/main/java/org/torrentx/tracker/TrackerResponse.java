package org.torrentx.tracker;

import org.torrentx.bencode.*;

import java.io.IOException;
import java.util.*;

/**
 * Represents a Tracker Response
 *
 * Parses the bencoded dictionary returned by the tracker.
 * Contains interval, peers, seeders/leechers count, etc.
 */
public class TrackerResponse {

    private final int interval;
    private final int minInterval;
    private final List<PeerAddress> peers;
    private final int complete;      // number of seeders
    private final int incomplete;    // number of leechers
    private final String failureReason;
    private final String warningMessage;

    private TrackerResponse(int interval, int minInterval, List<PeerAddress> peers,
                           int complete, int incomplete, String failureReason,
                           String warningMessage) {
        this.interval = interval;
        this.minInterval = minInterval;
        this.peers = peers;
        this.complete = complete;
        this.incomplete = incomplete;
        this.failureReason = failureReason;
        this.warningMessage = warningMessage;
    }

    /**
     * Parse tracker response from bencoded dictionary
     */
    public static TrackerResponse fromBDict(BDict dict) throws IOException {
        Map<String, BElement> map = dict.getMap();

        // Check for failure
        if (map.containsKey("failure reason")) {
            BElement elem = map.get("failure reason");
            if (elem instanceof BString) {
                String reason = ((BString) elem).asString();
                throw new IOException("Tracker error: " + reason);
            }
        }

        // Get interval
        int interval = 1800; // default 30 minutes
        if (map.containsKey("interval")) {
            BElement elem = map.get("interval");
            if (elem instanceof BInt) {
                interval = (int) ((BInt) elem).getValue();
            }
        }

        // Get min interval
        int minInterval = interval;
        if (map.containsKey("min interval")) {
            BElement elem = map.get("min interval");
            if (elem instanceof BInt) {
                minInterval = (int) ((BInt) elem).getValue();
            }
        }

        // Get complete (seeders)
        int complete = 0;
        if (map.containsKey("complete")) {
            BElement elem = map.get("complete");
            if (elem instanceof BInt) {
                complete = (int) ((BInt) elem).getValue();
            }
        }

        // Get incomplete (leechers)
        int incomplete = 0;
        if (map.containsKey("incomplete")) {
            BElement elem = map.get("incomplete");
            if (elem instanceof BInt) {
                incomplete = (int) ((BInt) elem).getValue();
            }
        }

        // Get warning message
        String warningMessage = null;
        if (map.containsKey("warning message")) {
            BElement elem = map.get("warning message");
            if (elem instanceof BString) {
                warningMessage = ((BString) elem).asString();
            }
        }

        // Parse peers
        List<PeerAddress> peers = new ArrayList<>();
        if (map.containsKey("peers")) {
            BElement peersElem = map.get("peers");

            if (peersElem instanceof BString) {
                // Compact format: 6 bytes per peer (4 bytes IP + 2 bytes port)
                peers = parsePeersCompact(((BString) peersElem).getBytes());
            } else if (peersElem instanceof BList) {
                // Dictionary format
                peers = parsePeersDictionary(((BList) peersElem).getValues());
            }
        }

        return new TrackerResponse(interval, minInterval, peers, complete, incomplete,
                                  null, warningMessage);
    }

    /**
     * Parse peers in compact format (6 bytes per peer)
     */
    private static List<PeerAddress> parsePeersCompact(byte[] data) {
        List<PeerAddress> peers = new ArrayList<>();

        if (data.length % 6 != 0) {
            throw new IllegalArgumentException("Compact peers data must be multiple of 6 bytes");
        }

        for (int i = 0; i < data.length; i += 6) {
            // IP: 4 bytes
            String ip = String.format("%d.%d.%d.%d",
                data[i] & 0xFF,
                data[i + 1] & 0xFF,
                data[i + 2] & 0xFF,
                data[i + 3] & 0xFF);

            // Port: 2 bytes (big-endian)
            int port = ((data[i + 4] & 0xFF) << 8) | (data[i + 5] & 0xFF);

            peers.add(new PeerAddress(ip, port));
        }

        return peers;
    }

    /**
     * Parse peers in dictionary format
     */
    private static List<PeerAddress> parsePeersDictionary(List<BElement> peersList) {
        List<PeerAddress> peers = new ArrayList<>();

        for (BElement peerElem : peersList) {
            if (peerElem instanceof BDict) {
                BDict peerDict = (BDict) peerElem;
                Map<String, BElement> peerMap = peerDict.getMap();

                String ip = "";
                int port = 0;

                if (peerMap.containsKey("ip")) {
                    BElement elem = peerMap.get("ip");
                    if (elem instanceof BString) {
                        ip = ((BString) elem).asString();
                    }
                }

                if (peerMap.containsKey("port")) {
                    BElement elem = peerMap.get("port");
                    if (elem instanceof BInt) {
                        port = (int) ((BInt) elem).getValue();
                    }
                }

                if (!ip.isEmpty() && port > 0) {
                    peers.add(new PeerAddress(ip, port));
                }
            }
        }

        return peers;
    }

    // Getters
    public int getInterval() {
        return interval;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public List<PeerAddress> getPeers() {
        return new ArrayList<>(peers);
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    @Override
    public String toString() {
        return "TrackerResponse{" +
                "interval=" + interval +
                ", peers=" + peers.size() +
                ", seeders=" + complete +
                ", leechers=" + incomplete +
                '}';
    }
}


