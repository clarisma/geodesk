/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.clarisma.common.store.BlobExporter;
import com.clarisma.common.util.Log;
import com.clarisma.common.util.ProgressListener;
import com.geodesk.core.Box;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// TODO:
//  - write temp file, rename to actual file when complete
//  - skip tiles that are already present
//  - check if exported meta tile is compatible
//
// TODO: Count:
//  - tiles skipped because missing
//  - tiles already present
//  - tiles actually written

// TODO: move more functionality to BlobExporter

// TODO: move to gol-tool?

public class TileExporter extends BlobExporter<FeatureStore>
{
    private volatile Exception error;
    private final Path exportPath;
    private final ProgressListener progress;

    public TileExporter(FeatureStore store, Path exportPath, ProgressListener progress)
    {
        super(store);
        this.exportPath = exportPath;
        this.progress = progress;
    }

    @Override protected void resetMetadata(ByteBuffer buf)
    {
        super.resetMetadata(buf);

        // Reset each tile-index entry
        int pTileIndex = store.tileIndexPointer();
        TileIndexWalker walker = new TileIndexWalker(store);
        walker.start(Box.ofWorld());
        while(walker.next())
        {
            buf.putInt(pTileIndex + walker.tip() * 4, 0);
        }

        // reset the purgatory tile
        buf.putInt(pTileIndex, 0);
    }

    public void exportTiles(IntList tiles) throws IOException
    {
        Set<Integer> foldersChecked = new HashSet<>();
        int threadCount = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threadCount, threadCount, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(threadCount * 4),
            new ThreadPoolExecutor.CallerRunsPolicy());

        executor.submit(new Task(exportPath.resolve("meta.tile"), -1));
        IntIterator iter = tiles.intIterator();
        try
        {
            while (iter.hasNext())
            {
                int tip = iter.next();
                Integer folder = tip >>> 12;
                if (!foldersChecked.contains(folder))
                {
                    Path folderPath = Tip.folder(exportPath, tip);
                    if(!Files.exists(folderPath))
                    {
                        Files.createDirectory(folderPath);
                    }
                    foldersChecked.add(folder);
                }
                executor.submit(new Task(Tip.path(exportPath, tip, ".tile"), tip));
            }
        }
        finally
        {
            executor.shutdown();
            try
            {
                executor.awaitTermination(30, TimeUnit.DAYS);
            }
            catch (InterruptedException ex)
            {
                // don't care about being interrupted, we're done anyway
            }
            progress.finished();
        }
    }

    private class Task implements Runnable
    {
        private final Path path;
        private final int id;

        public Task(Path path, int id)
        {
            this.path = path;
            this.id = id;
        }

        @Override public void run()
        {
            try
            {
                if(Files.exists(path)) return;
                int startPage;
                if(id == -1)
                {
                    startPage = 0;
                }
                else
                {
                    startPage = store.fetchTile(id);
                    assert startPage != 0;
                }
                // Log.debug("%d: %d", id, startPage);
                export(path, id, startPage);
            }
            catch(Exception ex)
            {
                error = ex;
                // Log.error(ex.toString());
            }
            progress.progress(1);      // TODO: errors, skipped tiles
        }
    }

    public Exception error()
    {
        return error;
    }
}
