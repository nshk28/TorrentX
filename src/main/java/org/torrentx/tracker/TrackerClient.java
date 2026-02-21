package org.torrentx.tracker;

import org.torrentx.bencode.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Tracker Client for BitTorrent Protocol
 *
 * Communicates with tracker servers to announce torrent status and get peer lists.
 * Supports HTTP trackers (UDP trackers can be added in future versions).
 *
 * IMPLEMENTATION SUMMARY:
 * =====================
 * This module handles all tracker communication for the BitTorrent client.
 *
 * Features:
 * - Send announce requests to tracker servers
 * - Parse bencoded tracker responses
 * - Extract peer lists (compact and dictionary formats)
 * - Handle tracker errors and retries
 * - Report upload/download statistics
 * - Support event-based announcing (started, completed, stopped)
 *
 * Usage:
 *   TrackerClient client = new TrackerClient();
 *   List<PeerAddress> peers = client.announce(
 *       "http://tracker.example.com:6969/announce",
 *       infoHash,
 *       peerId,
 *       port,
 *       uploaded,
 *       downloaded,
 *       left
 *   );
 *
 * Tracker Request Format:
 * GET /announce?
 *   info_hash=<20-byte-value>
 *   &peer_id=<20-byte-value>
 *   &port=<listening-port>
 *   &uploaded=<bytes>
 *   &downloaded=<bytes>
 *   &left=<bytes-remaining>
 *   &compact=1
 *   &event=started|completed|stopped
 *
 * Tracker Response (bencoded):
 * {
 *   "interval": <seconds>,
 *   "min interval": <seconds>,
 *   "peers": [
 *     {
 *       "ip": "<ip>",
 *       "port": <port>,
 *       "peer id": "<peer-id>"
 *     }
 *   ],
 *   "complete": <seeders>,
 *   "incomplete": <leechers>
 * }
 */
public class TrackerClient {

    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final String USER_AGENT = "TorrentX/1.0 (Java BitTorrent Client)";

    /**
     * Announce to tracker and get peer list
     */
    public TrackerResponse announce(String announceUrl, byte[] infoHash, byte[] peerId,
                                     int port, long uploaded, long downloaded, long left)
            throws IOException {
        return announce(announceUrl, infoHash, peerId, port, uploaded, downloaded, left, null);
    }

    /**
     * Announce to tracker with optional event
     * @param event "started", "stopped", "completed", or null
     */
    public TrackerResponse announce(String announceUrl, byte[] infoHash, byte[] peerId,
                                     int port, long uploaded, long downloaded, long left,
                                     String event) throws IOException {

        // Build tracker request URL
        String requestUrl = buildTrackerUrl(announceUrl, infoHash, peerId, port,
                                           uploaded, downloaded, left, event);

        // Send HTTP request to tracker
        byte[] response = sendHttpRequest(requestUrl);

        // Parse bencoded response
        BencodeParser parser = new BencodeParser(response);
        BElement element = parser.parse();

        if (!(element instanceof BDict)) {
            throw new IOException("Tracker response must be a dictionary");
        }

        // Parse response into TrackerResponse object
        return TrackerResponse.fromBDict((BDict) element);
    }

    /**
     * Build tracker URL with parameters
     */
    private String buildTrackerUrl(String baseUrl, byte[] infoHash, byte[] peerId,
                                   int port, long uploaded, long downloaded, long left,
                                   String event) throws IOException {

        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?");

        // Add parameters
        url.append("info_hash=").append(urlEncode(infoHash));
        url.append("&peer_id=").append(urlEncode(peerId));
        url.append("&port=").append(port);
        url.append("&uploaded=").append(uploaded);
        url.append("&downloaded=").append(downloaded);
        url.append("&left=").append(left);
        url.append("&compact=1");

        if (event != null && !event.isEmpty()) {
            url.append("&event=").append(event);
        }

        return url.toString();
    }

    /**
     * URL encode bytes for tracker request
     */
    private String urlEncode(byte[] data) throws IOException {
        StringBuilder encoded = new StringBuilder();
        for (byte b : data) {
            encoded.append("%");
            encoded.append(String.format("%02X", b));
        }
        return encoded.toString();
    }

    /**
     * Send HTTP GET request to tracker
     */
    private byte[] sendHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Tracker returned HTTP " + responseCode);
            }

            // Read response
            InputStream in = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            in.close();
            return out.toByteArray();

        } finally {
            connection.disconnect();
        }
    }
}


