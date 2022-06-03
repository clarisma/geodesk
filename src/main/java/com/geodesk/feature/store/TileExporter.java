package com.geodesk.feature.store;

import com.clarisma.common.store.BlobExporter;
import com.geodesk.core.Box;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class TileExporter extends BlobExporter<FeatureStore>
{
    private volatile Throwable error;
    private int currentSuperFolder;
    private Path exportPath;
    private ThreadPoolExecutor executor;
    private int tilesExported;

    public TileExporter(FeatureStore store)
    {
        super(store);
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

    private synchronized void updateProgress(int exported)
    {
        tilesExported += exported;
        System.err.format("%d tile%s saved...\r", tilesExported,
            tilesExported==1 ? "" : "s");
    }

    private void report()
    {
        System.err.format("%d tile%s saved.  \n", tilesExported,
            tilesExported==1 ? "" : "s");
    }

    private void exportTile(int tip, int tilePage) throws IOException
    {
        int superFolder = tip >>> 12;
        if(superFolder != currentSuperFolder)
        {
            currentSuperFolder = superFolder;
            Path folder = Tip.folder(exportPath, tip);
            if(!Files.exists(folder))
            {
                Files.createDirectories(folder);
            }
        }
        Path filePath = Tip.path(exportPath, tip, ".tile");
        executor.submit(new Task(filePath, tilePage));
    }

    public void export(Path exportPath, Bounds bbox, Filter filter) throws IOException
    {
        this.exportPath = exportPath;
        error = null;
        currentSuperFolder = -1;
        tilesExported = 0;

        int threadCount = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(
            threadCount, threadCount, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(threadCount * 4),
            new ThreadPoolExecutor.CallerRunsPolicy());

        if(Files.notExists(exportPath)) Files.createDirectories(exportPath);

        // TODO: Check presence of meta, and if compatible
        executor.submit(new Task(exportPath.resolve("meta.tile"), 0));

        int purgatoryPage = store.tilePage(0);
        if(purgatoryPage > 0)
        {
            exportTile(0, purgatoryPage);
        }

        TileIndexWalker walker = new TileIndexWalker(store);
        // TODO: bbox and/or filter
        walker.start(bbox);
        while(walker.next())
        {
            int tilePage = walker.tilePage();
            if (tilePage != 0) exportTile(walker.tip(), tilePage);
        }

        executor.shutdown();
        try
        {
            executor.awaitTermination(30, TimeUnit.DAYS);
        }
        catch(InterruptedException ex)
        {
            // do nothing
        }
        report();
        executor = null;
    }

    private class Task implements Runnable
    {
        private final Path path;
        private final int startPage;

        public Task(Path path, int startPage)
        {
            this.path = path;
            this.startPage = startPage;
        }

        @Override public void run()
        {
            try
            {
                export(path, startPage);
            }
            catch(Throwable ex)
            {
                error = ex;
            }
            updateProgress(1);      // TODO: errors, skipped tiles
        }
    }
}
