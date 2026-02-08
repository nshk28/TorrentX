package org.torrentx.bencode;

public class BInt implements BElement {
    private final long value;


    public BInt(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "BInt(" + value + ")";
    }
}
