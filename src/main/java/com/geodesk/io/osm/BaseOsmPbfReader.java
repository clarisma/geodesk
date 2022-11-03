/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io.osm;

import com.geodesk.feature.Tags;


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

}
