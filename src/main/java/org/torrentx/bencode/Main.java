package org.torrentx.bencode;

public class Main {
    public static void main(String[] args) throws Exception {
        String data = "d3:cow3:moo4:spam4:eggse";

        BencodeParser parser = new BencodeParser(data.getBytes());
        BElement result = parser.parse();

        System.out.println(result);
    }
}
