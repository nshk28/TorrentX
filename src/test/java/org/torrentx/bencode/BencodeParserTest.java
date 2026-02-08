package org.torrentx.bencode;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BencodeParserTest {

    private BElement parse(String s) throws IOException {
        return new BencodeParser(s.getBytes(StandardCharsets.UTF_8)).parse();
    }

    @Test
    void testInteger() throws IOException {
        BElement e = parse("i42e");

        assertTrue(e instanceof BInt);
        assertEquals(42, ((BInt) e).getValue());
    }

    @Test
    void testString() throws IOException {
        BElement e = parse("4:spam");

        assertTrue(e instanceof BString);
        assertEquals("spam", new String(((BString) e).getBytes()));
    }

    @Test
    void testList() throws IOException {
        BElement e = parse("l4:spam4:eggse");

        assertTrue(e instanceof BList);
        BList list = (BList) e;

        assertEquals(2, list.getValues().size());

        assertEquals("spam",
                ((BString) list.getValues().get(0)).asString());

        assertEquals("eggs",
                ((BString) list.getValues().get(1)).asString());

    }

    @Test
    void testDictionary() throws IOException {
        BElement e = parse("d3:cow3:moo4:spam4:eggse");

        assertTrue(e instanceof BDict);
        BDict dict = (BDict) e;

        assertEquals("moo",
                ((BString) dict.getMap().get("cow")).asString());

        assertEquals("eggs",
                ((BString) dict.getMap().get("spam")).asString());

    }

}
