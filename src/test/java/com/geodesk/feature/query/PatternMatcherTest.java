package com.geodesk.feature.query;

import com.clarisma.common.util.Bytes;

import java.nio.charset.StandardCharsets;
import static java.lang.System.out;

public class PatternMatcherTest
{
    static void match(String candidate, String match)
    {
        byte[] cBytes = candidate.getBytes(StandardCharsets.UTF_8);
        byte[] mBytes = match.getBytes(StandardCharsets.UTF_8);
        int n = Bytes.indexOf(cBytes, mBytes);
        out.format("'%s' is %s '%s'\n", match, n >= 0 ? "in" : "not in", candidate);
    }

    public static void main(String[] args)
    {
        match("monkeykey", "keykey");
        match("monkey", "key");
        match("monkey", "money");
        match("money", "key");

    }
}
