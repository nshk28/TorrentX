package org.torrentx.torrent;

import org.torrentx.bencode.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Represents a parsed .torrent file
 * Handles reading and parsing bencoded torrent metadata
 */
public class TorrentFile {
    private final MetaInfo metaInfo;

    private TorrentFile(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    /**
     * Load a torrent file from disk
     * @param path Path to the .torrent file
     * @return TorrentFile object
     * @throws IOException if file cannot be read
     */
    public static TorrentFile load(String path) throws IOException {
        return load(new File(path));
    }

    /**
     * Load a torrent file from disk
     * @param file The torrent file
     * @return TorrentFile object
     * @throws IOException if file cannot be read
     */
    public static TorrentFile load(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        return load(data);
    }

    /**
     * Load a torrent file from byte array
     * @param data The bencoded torrent data
     * @return TorrentFile object
     * @throws IOException if parsing fails
     */
    public static TorrentFile load(byte[] data) throws IOException {
        BencodeParser parser = new BencodeParser(data);
        BElement element = parser.parse();

        if (!(element instanceof BDict)) {
            throw new IOException("Torrent file must be a dictionary");
        }

        BDict dict = (BDict) element;
        MetaInfo metaInfo = MetaInfo.fromBDict(dict);
        return new TorrentFile(metaInfo);
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public String getAnnounce() {
        return metaInfo.getAnnounce();
    }

    public List<String> getAnnounceList() {
        return metaInfo.getAnnounceList();
    }

    public String getName() {
        return metaInfo.getName();
    }

    public long getLength() {
        return metaInfo.getLength();
    }

    public int getPieceLength() {
        return metaInfo.getPieceLength();
    }

    public List<PieceInfo> getPieces() {
        return metaInfo.getPieces();
    }

    public List<FileInfo> getFiles() {
        return metaInfo.getFiles();
    }

    public byte[] getInfoHash() {
        return metaInfo.getInfoHash();
    }

    public String getInfoHashHex() {
        return bytesToHex(getInfoHash());
    }

    public boolean isMultiFile() {
        return metaInfo.isMultiFile();
    }

    public int getTotalPieces() {
        return metaInfo.getPieces().size();
    }

    @Override
    public String toString() {
        return "TorrentFile{" +
                "name='" + getName() + '\'' +
                ", announce='" + getAnnounce() + '\'' +
                ", length=" + getLength() +
                ", pieceLength=" + getPieceLength() +
                ", pieces=" + getTotalPieces() +
                '}';
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}


