package com.geodesk.feature.query;

import com.clarisma.common.util.Log;
import com.geodesk.feature.match.MatcherCoder;
import com.geodesk.feature.match.MatcherParser;
import com.geodesk.feature.match.MatcherXmlWriter;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.util.Stopwatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class QueryParserTest
{
    MatcherParser parser;
    MatcherCoder coder;
    String[] codesToStrings;
    ObjectIntMap<String> stringsToCodes;

    @Before
    public void setUp() throws Exception
    {
        loadStrings();
        parser = new MatcherParser(stringsToCodes, null); // TODO
        coder = new MatcherCoder(stringsToCodes.get("no"));
    }

    private void loadStrings() throws IOException
    {
        MutableObjectIntMap<String> stringMap = ObjectIntMaps.mutable.empty();
        List<String> strings = new ArrayList<>();
        strings.add("");
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream("feature/strings.txt")));
        for (;;)
        {
            String s = reader.readLine();
            if (s == null) break;
            stringMap.put(s, strings.size());	// 1-based index
            strings.add(s);
        }
        codesToStrings = strings.toArray(new String[0]);
        stringsToCodes = stringMap;
    }

    private void dumpQuery()
    {
        // Uncomment for debugging
        /*
        MatcherXmlWriter out = new MatcherXmlWriter(System.out);
        out.writeQuery(parser.query());
        out.flush();
         */
    }

    @Test
    public void testQuery()
    {
        parser.parse(
            "na[amenity=pub,bar,cafe,restaurant][local_key != 'banana']," +
            "n[emergency]," +
            "wa[maxspeed='*mph'][maxspeed < 35][maxspeed < 4][maxspeed = 10]");
        dumpQuery();
    }

    @Test
    public void testQuery2()
    {
        parser.parse("na[amenity=restaurant][cuisine=greek][name='Acro*','Akro*']");
        dumpQuery();
    }

    @Test
    public void testQueryParserPerformance()
    {
        Stopwatch timer = new Stopwatch();
        int runs = 10_000_000;
        timer.start();
        for(int i=0; i<runs; i++)
        {
            parser.parse("na[amenity=pub,bar,cafe,restaurant][local_key != 'banana']");
        }
        Log.debug("Parsed %d queries in %d ms", runs, timer.stop());

    }

    @Test
    public void testQueryCoderPerformance()
    {
        Stopwatch timer = new Stopwatch();
        int runs = 10_000;
        long bytesEncoded = 0;
        timer.start();
        for(int i=0; i<runs; i++)
        {
            parser.parse("na[amenity=pub,bar,cafe,restaurant][local_key != 'banana']");
            // parser.parse("w[natural=coastline]");
            bytesEncoded += coder.createMatcherClass("Test", parser.query()).length;
        }
        Log.debug("Parsed and coded %d queries in %d ms (%d bytes per class)",
            runs, timer.stop(), bytesEncoded / runs);

    }
}