package org.torrentx.torrent;

import org.torrentx.bencode.BDict;
import org.torrentx.bencode.BElement;
import org.torrentx.bencode.BInt;
import org.torrentx.bencode.BList;
import org.torrentx.bencode.BString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a file within a torrent
 * For multi-file torrents, contains path components and size
 */
public class FileInfo {
    private final List<String> path; // Path components (e.g., ["folder", "file.txt"])
    private final long length;

    public FileInfo(List<String> path, long length) {
        this.path = new ArrayList<>(path);
        this.length = length;
    }

    /**
     * Parse FileInfo from a bencoded dictionary
     */
    public static FileInfo fromBDict(BDict dict) throws IOException {
        Map<String, BElement> map = dict.getMap();

        // Get length
        long length = 0;
        if (map.containsKey("length")) {
            BElement elem = map.get("length");
            if (elem instanceof BInt) {
                length = ((BInt) elem).getValue();
            }
        }

        // Get path
        List<String> path = new ArrayList<>();
        if (map.containsKey("path")) {
            BElement elem = map.get("path");
            if (elem instanceof BList) {
                BList pathList = (BList) elem;
                for (BElement pathElem : pathList.getValues()) {
                    if (pathElem instanceof BString) {
                        path.add(((BString) pathElem).asString());
                    }
                }
            }
        }

        return new FileInfo(path, length);
    }

    public List<String> getPath() {
        return new ArrayList<>(path);
    }

    public String getPathString() {
        return String.join("/", path);
    }

    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "path=" + getPathString() +
                ", length=" + length +
                '}';
    }
}


