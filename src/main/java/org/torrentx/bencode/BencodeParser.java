
package org.torrentx.bencode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class BencodeParser {

    private final ByteArrayInputStream in;

    public BencodeParser(byte[] data) {
        this.in = new ByteArrayInputStream(data);
    }

    public BElement parse() throws IOException {
        int c = in.read();
        if (c == -1) throw new IOException("Unexpected end");

        if (c == 'i') return parseInt();
        if (c == 'l') return parseList();
        if (c == 'd') return parseDict();
        if (Character.isDigit(c)) return parseString(c);

        throw new IOException("Invalid bencode prefix: " + (char) c);
    }

    private BInt parseInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != 'e') {
            if (c == -1) throw new IOException("Unterminated int");
            sb.append((char) c);
        }
        return new BInt(Long.parseLong(sb.toString()));
    }

    private BString parseString(int firstDigit) throws IOException {
        int length = firstDigit - '0';
        int c;

        while ((c = in.read()) != ':') {
            if (c == -1) throw new IOException("Bad string length");
            length = length * 10 + (c - '0');
        }

        byte[] buf = in.readNBytes(length);
        if (buf.length != length) {
            throw new IOException("Unexpected EOF in string");
        }

        return new BString(buf);
    }

    private BList parseList() throws IOException {
        List<BElement> list = new ArrayList<>();

        while (true) {
            in.mark(1);
            int c = in.read();
            if (c == 'e') break;
            in.reset();
            list.add(parse());
        }

        return new BList(list);
    }

    private BDict parseDict() throws IOException {
        Map<String, BElement> map = new LinkedHashMap<>();

        while (true) {
            in.mark(1);
            int c = in.read();
            if (c == 'e') break;
            in.reset();

            BString key = (BString) parse();
            BElement value = parse();

            map.put(key.asString(), value);
        }

        return new BDict(map);
    }
}
