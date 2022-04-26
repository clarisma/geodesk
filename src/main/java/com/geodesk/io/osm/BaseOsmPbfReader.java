package com.geodesk.io.osm;

import com.geodesk.feature.Tags;
import org.locationtech.jts.util.Stopwatch;

import java.io.IOException;

public class BaseOsmPbfReader extends OsmPbfReader
{
    protected WorkerThread createWorker()
    {
        return new SimpleThread();
    }

    private class SimpleThread extends WorkerThread
    {
        @Override protected void beginNodes()
        {
            log("Reading nodes...");
        }

        @Override protected void node(long id, int x, int y, Tags tags)
        {

        }

        @Override protected void endNodes()
        {
            log("Finished reading nodes.");
        }

        @Override protected void beginWays()
        {
            log("Reading ways...");
        }

        @Override protected void way(long id, Tags tags, Nodes nodes)
        {

        }

        @Override protected void endWays()
        {
            log("Finished reading ways.");
        }

        @Override protected void beginRelations()
        {
            log("Reading relations...");
        }

        @Override protected void relation(long id, Tags tags, Members members)
        {

        }

        @Override protected void endRelations()
        {
            log("Finished reading relations.");
        }
    }

    public static void test(String mapDataFile) throws IOException
    {
        log.info("Counting nodes...");
        Stopwatch timer = new Stopwatch();
        timer.start();
        BaseOsmPbfReader reader = new BaseOsmPbfReader();
        reader.read(mapDataFile);
        log.info("Completed in {} ms\n", timer.stop());
    }

    public static void main(String[] args) throws Exception
    {
        // test("c:\\velojoe\\mapdata\\planet.osm.pbf");
        // test("c:\\geodesk\\mapdata\\planet.osm.pbf");
        test("c:\\geodesk\\mapdata\\de-2021-01-29.osm.pbf");
        // test("/home/md/geodesk/mapdata/planet.osm.pbf");
    }

}
