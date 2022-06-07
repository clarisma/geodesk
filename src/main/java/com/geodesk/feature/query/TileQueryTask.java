package com.geodesk.feature.query;

import com.clarisma.common.util.Log;
import com.geodesk.core.Tile;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.FeatureStore;

import java.nio.ByteBuffer;

import static com.geodesk.feature.match.TypeBits.*;

public class TileQueryTask extends QueryTask
{
    private final int tile;
    private final int tip;
    private int bboxFlags;
    private int tilesProcessed;

    public TileQueryTask(Query query, int tile, int tip)
    {
        super(query);
        this.tile = tile;
        this.tip = tip;
    }

    private RTreeQueryTask searchRTree(ByteBuffer buf, int ppTree, Matcher filter, RTreeQueryTask task)
    {
        int p = buf.getInt(ppTree);
        if(p == 0) return task;
        if((p & 1) == 0)
        {
            task = new RTreeQueryTask(query, buf, ppTree, bboxFlags, filter, task);
            task.fork();
            return task;
        }
        assert (p & 2) == 0;
        p = ppTree + (p ^ 1);
        for(;;)
        {
            int last = buf.getInt(p) & 1;
            int keyBits = buf.getInt(p+4);
            if(filter.acceptIndex(keyBits))
            {
                task = new RTreeQueryTask(query, buf, p, bboxFlags, filter, task);
                task.fork();
            }
            if(last != 0) break;
            p += 8;
        }
        return task;
    }

    private RTreeQueryTask searchNodeRTree(ByteBuffer buf, int ppTree, Matcher filter, RTreeQueryTask task)
    {
        int p = buf.getInt(ppTree);
        if(p == 0) return task;
        if((p & 1) == 0)
        {
            task = new RTreeQueryTask.Nodes(query, buf, ppTree, filter, task);
            task.fork();
            return task;
        }
        p = ppTree + (p ^ 1);
        for(;;)
        {
            int last = buf.getInt(p) & 1;
            int keyBits = buf.getInt(p+4);
            if(filter.acceptIndex(keyBits))
            {
                task = new RTreeQueryTask.Nodes(query, buf, p, filter, task);
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
            ByteBuffer buf = store.bufferOfPage(tilePage);
            int pTile = store.offsetOfPage(tilePage);

            // TODO: could calculate these without branching:
            //  north - maxY, shift sign bit to flag

            int north = Tile.topY(tile);
            int west = Tile.leftX(tile);
            bboxFlags = ((query.maxY() > north) ? FeatureFlags.MULTITILE_NORTH : 0) |
                ((query.minX() < west) ? FeatureFlags.MULTITILE_WEST : 0);

            MatcherSet filters = query.matchers();
            RTreeQueryTask task = null;

            /*
            Filter f;
            if ((f = filters.nodes()) != null) task = searchNodeRTree(buf, pTile + 8, f, task);
            if ((f = filters.ways()) != null) task = searchRTree(buf, pTile + 12, f, task);
            if ((f = filters.areas()) != null) task = searchRTree(buf, pTile + 16, f, task);
            if ((f = filters.relations()) != null) task = searchRTree(buf, pTile + 20, f, task);
            */
            int types = query.types();
            if ((types & NODES) != 0)
            {
                task = searchNodeRTree(buf, pTile + 8, filters.nodes(), task);
            }
            if ((types & NONAREA_WAYS) != 0)
            {
                task = searchRTree(buf, pTile + 12, filters.ways(), task);
            }
            if ((types & AREAS) != 0)
            {
                task = searchRTree(buf, pTile + 16, filters.areas(), task);
            }
            if ((types & NONAREA_RELATIONS) != 0)
            {
                task = searchRTree(buf, pTile + 20, filters.relations(), task);
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
