package org.torrentx.bencode;

import java.nio.charset.StandardCharsets;

public class BString implements BElement {
    private final byte[] data;

    public BString(byte[] data) {
        this.data = data;
    }

    public String asString() {
        return new String(data, StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return "BString(" + asString() + ")";
    }
}
