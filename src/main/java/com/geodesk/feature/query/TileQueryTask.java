/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.Filter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Tile;

import java.nio.ByteBuffer;

import static com.geodesk.feature.match.TypeBits.*;

// TODO: Maybe give task the tile page instead of TIP
public class TileQueryTask extends QueryTask
{
    private final int tilePage;
    protected int bboxFlags;
    private int tilesProcessed;
    protected ByteBuffer buf;
    protected Filter filter;

    public TileQueryTask(Query query, int tilePage, int northwestFlags, Filter filter)
    {
        super(query);
        this.tilePage = tilePage;
        this.bboxFlags = northwestFlags;
        this.filter = filter;
        // Log.debug("Tile %s with filter %s", Tile.toString(tile), filter);
    }

    private RTreeQueryTask searchRTree(int ppTree, Matcher matcher, RTreeQueryTask task)
    {
        int p = buf.getInt(ppTree);
        if(p == 0) return task;
        p = ppTree + p;
        for(;;)
        {
            int last = buf.getInt(p) & 1;
            int keyBits = buf.getInt(p+4);
            if(matcher.acceptIndex(keyBits))
            {
                task = new RTreeQueryTask(this, p, matcher, task);
                task.fork();
            }
            if(last != 0) break;
            p += 8;
        }
        return task;
    }

    private RTreeQueryTask searchNodeRTree(int ppTree, Matcher matcher, RTreeQueryTask task)
    {
        int p = buf.getInt(ppTree);
        if(p == 0) return task;
        p = ppTree + p;
        for(;;)
        {
            int last = buf.getInt(p) & 1;
            int keyBits = buf.getInt(p+4);
            if(matcher.acceptIndex(keyBits))
            {
                task = new RTreeQueryTask.Nodes(this, p, matcher, task);
                task.fork();
            }
            if(last != 0) break;
            p += 8;
        }
        return task;
    }

    @Override protected boolean exec()
    {
        // System.out.format("Searching tile at page %d\n", tilePage);

        try
        {
            FeatureStore store = query.store();
            buf = store.bufferOfPage(tilePage);
            int pTile = store.offsetOfPage(tilePage);

            /*
            // TODO: could calculate these without branching:
            //  north - maxY, shift sign bit to flag
            int north = Tile.topY(tile);
            int west = Tile.leftX(tile);
            bboxFlags = ((query.maxY() > north) ? FeatureFlags.MULTITILE_NORTH : 0) |
                ((query.minX() < west) ? FeatureFlags.MULTITILE_WEST : 0);
             */

            Matcher matcher = query.matcher();
            RTreeQueryTask task = null;

            int types = query.types();
            if ((types & NODES) != 0)
            {
                task = searchNodeRTree(pTile + 8, matcher, task);
            }
            if ((types & NONAREA_WAYS) != 0)
            {
                task = searchRTree(pTile + 12, matcher, task);
            }
            if ((types & AREAS) != 0)
            {
                task = searchRTree(pTile + 16, matcher, task);
            }
            if ((types & NONAREA_RELATIONS) != 0)
            {
                task = searchRTree(pTile + 20, matcher, task);
            }

            QueryResults res = QueryResults.EMPTY;
            while (task != null)
            {
                res = QueryResults.merge(res, task.join());
                task = task.next;
            }
            results = res;
        }
        catch(Throwable ex)
        {
            /*
            Log.error("Failed: %s", ex);
            ex.printStackTrace();
             */
            query.setError(ex);
            results = QueryResults.EMPTY;
        }
        tilesProcessed = 1;
        query.put(this);
        return true;
    }

    public int tilesProcessed()
    {
        return tilesProcessed;
    }

    /*
    public void mergeWith(TileQueryTask other)
    {
        results = QueryResults.merge(results, other.results);
        tilesProcessed++;
    }
     */
}
