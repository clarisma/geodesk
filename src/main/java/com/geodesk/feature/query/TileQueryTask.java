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

import java.nio.ByteBuffer;

import static com.geodesk.feature.match.TypeBits.*;

public class TileQueryTask extends QueryTask
{
    // private final int tile;     // TODO: not needed, drop
    private final int tip;
    protected int bboxFlags;
    private int tilesProcessed;
    protected ByteBuffer buf;
    protected Filter filter;

    public TileQueryTask(Query query, /* int tile, */ int tip, int northwestFlags, Filter filter)
    {
        super(query);
        // this.tile = tile;
        this.tip = tip;
        this.bboxFlags = northwestFlags;
        this.filter = filter;
        // Log.debug("Tile %s with filter %s", Tile.toString(tile), filter);
    }

    private RTreeQueryTask searchRTree(int ppTree, Matcher matcher, RTreeQueryTask task)
    {
        int p = buf.getInt(ppTree);
        if(p == 0) return task;
        if((p & 1) == 0)
        {
            task = new RTreeQueryTask(this, ppTree, matcher, task);
            task.fork();
            return task;
        }
        assert (p & 2) == 0;
        p = ppTree + (p ^ 1);
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
        if((p & 1) == 0)
        {
            task = new RTreeQueryTask.Nodes(this, ppTree, matcher, task);
            task.fork();
            return task;
        }
        p = ppTree + (p ^ 1);
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
        // log.debug("Searching tile {} ({})", Tile.toString(tile), Tip.toString(tip));

        try
        {
            FeatureStore store = query.store();
            int tilePage = store.fetchTile(tip);
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
