package com.geodesk.feature.query;

import com.clarisma.common.util.Log;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.store.*;
import com.geodesk.geom.Bounds;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.*;

// TODO: Idea: Could use AtomicReference for head and use updateAndGet()
//  No, because does not block

public class Query implements Iterator<Feature>, Bounds
{
    private final FeatureStore store;
    private int minX;
    private final int minY;
    private int maxX;
    private final int maxY;
    private final int types;
    private final MatcherSet matchers;
    final Filter filter;
    private ExecutorService executor;
    // private TileQueryTask head;     // access must be synchronized
        // TODO: maybe put last, so we reduce false sharing (may be in
        //  different cache line from values that are frequently read
    private TileIndexWalker tileWalker;
    private QueryResults currentResults;
    private int currentPos;
    private Feature nextFeature;
    private MutableLongSet potentialDupes;
    private int pendingTiles;
    private boolean allTilesRequested;
    private BlockingQueue<TileQueryTask> queue;

    public Query(WorldView<?> view)
    {
        this.store = view.store;
        this.executor = store.executor();
        this.types = view.types;
        this.matchers = view.matchers;
        this.filter = view.filter;
        Bounds bbox = view.bbox;
        minX = bbox.minX();
        minY = bbox.minY();
        maxX = bbox.maxX();
        maxY = bbox.maxY();
        queue = new LinkedBlockingQueue<>();
        tileWalker = new TileIndexWalker(store.baseMapping(),
            store.tileIndexPointer(), store.zoomLevels());
        start();
    }

    public FeatureStore store()
    {
        return store;
    }

    public int types()
    {
        return types;
    }

    public MatcherSet matchers()
    {
        return matchers;
    }

    @Override public int minX()
    {
        return minX;
    }

    @Override public int minY()
    {
        return minY;
    }

    @Override public int maxX()
    {
        return maxX;
    }

    @Override public int maxY()
    {
        return maxY;
    }

    /*
    synchronized void put(TileQueryTask task)
    {
        if(head != null)
        {
            head.mergeWith(task);
            return;
        }
        head = task;
        notifyAll();        // TODO: notify() may have been the cause of deadlock!
    }

    private synchronized TileQueryTask take()
    {
        while(head == null)
        {
            // log.debug("Waiting for results... ({} tiles pending)", pendingTiles);
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                // do nothing
            }
        }
        TileQueryTask task = head;
        head = null;
        return task;
    }

     */

    void put(TileQueryTask task)
    {
        // TODO

        try
        {
            if(!queue.add(task))
            {
                Log.error("Couldn't add");
            }
        }
        catch (Exception e)
        {
            Log.error("%s", e);
            e.printStackTrace();
        }
    }

    TileQueryTask take()
    {
        try
        {
            return queue.take();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void start()
    {
        tileWalker.start(this);
        currentResults = QueryResults.EMPTY;
        currentPos = -1;

        // Submit initial tasks
        int maxPendingTiles = 8; // TODO
        while(pendingTiles < maxPendingTiles)
        {
            if(!tileWalker.next())
            {
                // We've traversed all tiles
                allTilesRequested = true;
                break;
            }
            requestTile();
        }
        fetchNext();
    }

    private void requestTile()
    {
        ForkJoinPool pool = (ForkJoinPool)executor; // TODO!
        pool.submit(new TileQueryTask(this, tileWalker.tile(), tileWalker.tip()));
        pendingTiles++;
        // if(pendingTiles > 10) log.debug("Requesting tile, {} pending", pendingTiles);
    }

    private void fetchNext()
    {
        currentPos++;
    main:
        for(;;)
        {
            if (currentPos >= currentResults.size)
            {
                // We're finished with the current batch of results

                currentPos = 0;
                if (currentResults.next == null)
                {
                    // We've consumed all retrieved results

                    if (pendingTiles == 0)
                    {
                        // no further tasks are pending, we're done
                        nextFeature = null;
                        return;
                    }

                    // Retrieve the next task from the queue, blocking if necessary

                    TileQueryTask task = take();
                    pendingTiles -= task.tilesProcessed();
                    if(!allTilesRequested)
                    {
                        if(tileWalker.next())
                        {
                            requestTile();
                        }
                        else
                        {
                            allTilesRequested = true;
                        }
                    }

                    currentResults = task.getRawResult();
                    continue;    // go back to loop since batch could be empty
                }
                currentResults = currentResults.next;
                continue;   // go back to loop since batch could be empty
            }

            ByteBuffer buf = currentResults.buf;
            int pFeature = currentResults.pointers[currentPos];
            int type = pFeature & 0x8000_0003;
            pFeature ^= type;
            for(;;)
            {
                if(type == 1)
                {
                    nextFeature = new StoredWay(store, buf, pFeature);
                    return;
                }
                if(type == 0)
                {
                    nextFeature = new StoredNode(store, buf, pFeature);
                    return;
                }
                if(type == 2)
                {
                    nextFeature = new StoredRelation(store, buf, pFeature);
                    return;
                }
                assert type != 0x8000_0003;
                long idBits = buf.getLong(pFeature) & 0xffff_ffff_ffff_ff18l;
                // isolate ID and type flags (bits 3 & 4)

					/*
					FeatureStore.log.debug("{}/{} requires explicit de-duplication",
						FeatureType.values()[type & 3].name().toLowerCase(),
						FeatureHandle.id(buf, ptr));
					*/

                /*
                log.debug("De-duping complex multi-tile feature: {}/{}",
                    FeatureId.typeToString(type & 3), StoredFeature.id(buf, pFeature));
                 */

                if(potentialDupes==null)
                {
                    potentialDupes = new LongHashSet();
                    potentialDupes.add(idBits);
                }
                else
                {
                    if(!potentialDupes.add(idBits))
                    {
                        currentPos++;       // TODO: check this for off-by-1 bugs
                        continue main;
                            // the label is after the initial currentPos++
                    }
                }
                type &= 3;
            }

        }

    }

    @Override public boolean hasNext()
    {
        return nextFeature != null;
    }

    @Override public Feature next()
    {
        Feature f = nextFeature;
        fetchNext();
        return f;
    }
}
