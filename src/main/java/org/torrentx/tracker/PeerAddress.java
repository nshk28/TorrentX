package org.torrentx.tracker;

import java.util.Objects;

/**
 * Represents a Peer Address (IP and Port)
 *
 * Returned by tracker in announce responses.
 * Used to connect to peers for downloading pieces.
 */
public class PeerAddress {

    private final String ip;
    private final int port;

    public PeerAddress(String ip, int port) {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("IP cannot be empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    /**
     * Get address as "ip:port" string
     */
    public String getAddress() {
        return ip + ":" + port;
    }

    @Override
    public String toString() {
        return getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerAddress that = (PeerAddress) o;
        return port == that.port && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}

