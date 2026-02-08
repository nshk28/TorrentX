package org.torrentx.bencode;

import java.util.List;

public class BList implements BElement {
    private final List<BElement> values;

    public BList(List<BElement> values) {
        this.values = values;
    }

    public List<BElement> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "BList" + values;
    }
}
