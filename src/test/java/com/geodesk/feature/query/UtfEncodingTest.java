package com.geodesk.feature.query;
import java.nio.charset.StandardCharsets;

import static java.lang.System.out;

public class UtfEncodingTest
{
    static void dumpEncoded(String s)
    {
        out.format("%s:\n", s);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        for(int i=0; i<bytes.length; i++)
        {
            out.format("  %d\n", bytes[i]);
        }
    }

    public static void main(String[] args)
    {
        dumpEncoded("Hühnerstraße");
        dumpEncoded("Hü");
        dumpEncoded("straße");
        dumpEncoded("ße");
    }
}
