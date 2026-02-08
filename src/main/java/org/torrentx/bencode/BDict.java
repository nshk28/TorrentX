package org.torrentx.bencode;

import java.util.Map;

public class BDict implements BElement {
    private final Map<String, BElement> map;

    public BDict(Map<String, BElement> map) {
        this.map = map;
    }

    public Map<String, BElement> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "BDict" + map;
    }
}
