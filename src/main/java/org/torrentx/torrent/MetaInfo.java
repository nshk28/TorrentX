package org.torrentx.torrent;

import org.torrentx.bencode.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Represents the metadata of a torrent file
 * Extracted from the bencoded dictionary
 */
public class MetaInfo {
    private final String announce;
    private final List<String> announceList;
    private final String name;
    private final long length;
    private final int pieceLength;
    private final List<PieceInfo> pieces;
    private final List<FileInfo> files;
    private final byte[] infoHash;
    private final boolean isMultiFile;

    private MetaInfo(String announce, List<String> announceList, String name,
                    long length, int pieceLength, List<PieceInfo> pieces,
                    List<FileInfo> files, byte[] infoHash, boolean isMultiFile) {
        this.announce = announce;
        this.announceList = announceList;
        this.name = name;
        this.length = length;
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.files = files;
        this.infoHash = infoHash;
        this.isMultiFile = isMultiFile;
    }

    /**
     * Parse MetaInfo from a bencoded dictionary
     */
    public static MetaInfo fromBDict(BDict dict) throws IOException {
        Map<String, BElement> map = dict.getMap();

        // Get announce URL
        String announce = "";
        if (map.containsKey("announce")) {
            BElement elem = map.get("announce");
            if (elem instanceof BString) {
                announce = ((BString) elem).asString();
            }
        }

        // Get announce list (optional)
        List<String> announceList = new ArrayList<>();
        if (map.containsKey("announce-list")) {
            BElement elem = map.get("announce-list");
            if (elem instanceof BList) {
                BList list = (BList) elem;
                for (BElement tier : list.getValues()) {
                    if (tier instanceof BList) {
                        BList tierList = (BList) tier;
                        for (BElement url : tierList.getValues()) {
                            if (url instanceof BString) {
                                announceList.add(((BString) url).asString());
                            }
                        }
                    }
                }
            }
        }
        if (announceList.isEmpty()) {
            announceList.add(announce);
        }

        // Get info dictionary
        if (!map.containsKey("info")) {
            throw new IOException("Torrent file missing 'info' dictionary");
        }

        BElement infoElem = map.get("info");
        if (!(infoElem instanceof BDict)) {
            throw new IOException("'info' must be a dictionary");
        }

        BDict info = (BDict) infoElem;
        byte[] infoHash = calculateInfoHash(info);

        Map<String, BElement> infoMap = info.getMap();

        // Get name
        String name = "";
        if (infoMap.containsKey("name")) {
            BElement elem = infoMap.get("name");
            if (elem instanceof BString) {
                name = ((BString) elem).asString();
            }
        }

        // Get piece length
        int pieceLength = 0;
        if (infoMap.containsKey("piece length")) {
            BElement elem = infoMap.get("piece length");
            if (elem instanceof BInt) {
                pieceLength = (int) ((BInt) elem).getValue();
            }
        }

        // Get pieces
        List<PieceInfo> pieces = new ArrayList<>();
        if (infoMap.containsKey("pieces")) {
            BElement elem = infoMap.get("pieces");
            if (elem instanceof BString) {
                byte[] piecesData = ((BString) elem).getBytes();
                pieces = parsePieces(piecesData);
            }
        }

        // Check if single or multi-file
        boolean isMultiFile = infoMap.containsKey("files");
        long totalLength = 0;
        List<FileInfo> files = new ArrayList<>();

        if (isMultiFile) {
            // Multi-file torrent
            BElement filesElem = infoMap.get("files");
            if (filesElem instanceof BList) {
                BList filesList = (BList) filesElem;
                for (BElement fileElem : filesList.getValues()) {
                    if (fileElem instanceof BDict) {
                        BDict fileDict = (BDict) fileElem;
                        FileInfo file = FileInfo.fromBDict(fileDict);
                        files.add(file);
                        totalLength += file.getLength();
                    }
                }
            }
        } else {
            // Single-file torrent
            if (infoMap.containsKey("length")) {
                BElement elem = infoMap.get("length");
                if (elem instanceof BInt) {
                    totalLength = ((BInt) elem).getValue();
                    files.add(new FileInfo(Arrays.asList(name), totalLength));
                }
            }
        }

        return new MetaInfo(announce, announceList, name, totalLength,
                pieceLength, pieces, files, infoHash, isMultiFile);
    }

    /**
     * Parse the pieces field (20 bytes per piece SHA-1 hash)
     */
    private static List<PieceInfo> parsePieces(byte[] piecesData) {
        List<PieceInfo> pieces = new ArrayList<>();
        if (piecesData.length % 20 != 0) {
            throw new IllegalArgumentException("Pieces data length must be multiple of 20");
        }

        for (int i = 0; i < piecesData.length; i += 20) {
            byte[] hash = new byte[20];
            System.arraycopy(piecesData, i, hash, 0, 20);
            pieces.add(new PieceInfo(pieces.size(), hash));
        }

        return pieces;
    }

    /**
     * Calculate SHA-1 hash of the info dictionary
     */
    private static byte[] calculateInfoHash(BDict info) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] infoBytes = bencodeDict(info);
            return digest.digest(infoBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Simple bencode encoding for dictionary (for info hash calculation)
     */
    private static byte[] bencodeDict(BDict dict) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("d");

        // Sort keys
        Map<String, BElement> sorted = new TreeMap<>(dict.getMap());

        for (Map.Entry<String, BElement> entry : sorted.entrySet()) {
            // Encode key
            String key = entry.getKey();
            sb.append(key.length()).append(":").append(key);

            // Encode value
            BElement value = entry.getValue();
            sb.append(bencode(value));
        }

        sb.append("e");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String bencode(BElement elem) {
        if (elem instanceof BInt) {
            return "i" + ((BInt) elem).getValue() + "e";
        } else if (elem instanceof BString) {
            byte[] bytes = ((BString) elem).getBytes();
            return bytes.length + ":" + new String(bytes, StandardCharsets.UTF_8);
        } else if (elem instanceof BList) {
            StringBuilder sb = new StringBuilder("l");
            for (BElement e : ((BList) elem).getValues()) {
                sb.append(bencode(e));
            }
            sb.append("e");
            return sb.toString();
        } else if (elem instanceof BDict) {
            // Simplified - just return empty dict for now
            return "de";
        }
        return "";
    }

    // Getters
    public String getAnnounce() {
        return announce;
    }

    public List<String> getAnnounceList() {
        return new ArrayList<>(announceList);
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public List<PieceInfo> getPieces() {
        return new ArrayList<>(pieces);
    }

    public List<FileInfo> getFiles() {
        return new ArrayList<>(files);
    }

    public byte[] getInfoHash() {
        return Arrays.copyOf(infoHash, infoHash.length);
    }

    public boolean isMultiFile() {
        return isMultiFile;
    }
}


