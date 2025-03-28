/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.clarisma.common.pbf.PbfDecoder;
import com.clarisma.common.store.BlobStore;
import com.clarisma.common.store.StoreException;
import com.geodesk.geom.Box;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherCompiler;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class FeatureStore extends BlobStore
{
    private int minZoom;
    private int zoomSteps;
    /**
     * A mapping of strings to their string-table code.
     * TODO: Currently, empty string is *not* in this map (since 0 means
     *  "this key is not a global string"); should we revise this definition?
     *
     *  TODO: No need to index more than 8K strings; we only look up keys,
     *   and key-codes cannot be > 8 K
     *    No! Query Compiler needs to look up values as well
     */
    private ObjectIntMap<String> stringsToCodes;
    /**
     * The global string table. Entry 0 is always an empty string ("")
     */
    private String[] codesToStrings;
    /**
     * A mapping of indexed keys (global-string code) to their index category
     * (1-based; range 1 to 30)
     */
    private IntIntMap keysToCategories;
    private ExecutorService executor;
    private MatcherCompiler matchers;
    private GeometryFactory geometryFactory;
    private int maxPendingTiles;

    public static final int MAGIC = 0x1CE50D6E;  // "geodesic"
    public static final int VERSION = 1_000_000;

    private static final int MAGIC_CODE_OFS = 32;
    public static final int ZOOM_LEVELS_OFS = 40;
    public static final int TILE_INDEX_PTR_OFS = 44;
    private static final int PROPERTIES_PTR_OFS = 48;
    private static final int STRING_TABLE_PTR_OFS = 52;
    private static final int INDEX_SCHEMA_PTR_OFS = 56;

    // TODO: move to subclass, don't need full functionality
    //  for the GolTool
    @Override protected void initialize() throws IOException
    {
        super.initialize();

        // TODO: when should this check be done?
        if(isEmpty() && downloader == null)
        {
            throw new StoreException("Empty library; " +
                "specify a URL from which its data can be downloaded.", path());
        }

        readStringTable();
        readIndexSchema();

        // Querying is enabled explicitly by the FeatureLibrary class, which
        // serves as the front-end of the API
        // enableQueries();
        int zoomLevels = zoomLevels();
        minZoom = ZoomLevels.minZoom(zoomLevels);
        zoomSteps = ZoomLevels.zoomSteps(zoomLevels);
    }

    /*
    protected ByteBuffer baseMapping()
    {
        return baseMapping;
    }

    protected ByteBuffer bufferOfPage(int page)
    {
        return super.bufferOfPage(page);
    }

    protected int offsetOfPage(int page)
    {
        return super.offsetOfPage(page);
    }
     */

    public int zoomLevels()
    {
        return baseMapping.getInt(ZOOM_LEVELS_OFS);
    }

    private void readStringTable()
    {
        int p = baseMapping.getInt(STRING_TABLE_PTR_OFS) + STRING_TABLE_PTR_OFS;
        PbfDecoder reader = new PbfDecoder(baseMapping, p);
        int count = (int) reader.readVarint();
        codesToStrings = new String[count + 1];
        codesToStrings[0] = "";

        // TODO: does this make sense? Doesn't the map already have a load factor?
        //  (But higher capacity may reduce hash collisions and make lookup more efficient)
        MutableObjectIntMap<String> stringMap =
            new ObjectIntHashMap<>(count + (count >> 1));
        stringMap.put("", 0);

        // TODO: Check if we need to have "" in this map (currently not included)
        //  1/11/23: Changed to include "" as entry 0
        //  (to determine if a key is not in the table, must supply -1 as default)

        // TODO: only index first 8K strings
        //  No! Query Compiler needs to look up values as well

        for (int i = 1; i <= count; i++)
        {
            String s = reader.readString();
            codesToStrings[i] = s;
            stringMap.put(s, i);
        }
        stringsToCodes = stringMap;
    }

    private void readIndexSchema()
    {
        int p = baseMapping.getInt(INDEX_SCHEMA_PTR_OFS) + INDEX_SCHEMA_PTR_OFS;
        int count = baseMapping.getInt(p);
        MutableIntIntMap map = new IntIntHashMap(count);
        for (int i = 0; i < count; i++)
        {
            p += 4;
            int entry = baseMapping.getInt(p);
            map.put((char) entry, entry >> 16);
        }
        keysToCategories = map;
    }

    public void enableQueries()
    {
        // TODO: guard against multiple calls
        matchers = new MatcherCompiler(stringsToCodes, codesToStrings, keysToCategories);
        executor = new ForkJoinPool();// TODO: ability to set parallelism
        maxPendingTiles = Runtime.getRuntime().availableProcessors() * 2;
        geometryFactory = new GeometryFactory(); // TODO
    }

    public ExecutorService executor()
    {
        return executor;
    }

    public int maxPendingTiles() { return maxPendingTiles; }

    public int tileIndexPointer()
    {
        return baseMapping.getInt(TILE_INDEX_PTR_OFS) + TILE_INDEX_PTR_OFS;
    }

    // TODO: this is hacky, needed for TileCatalog in GOL Tool
    public static int tileIndexPointer(ByteBuffer buf)
    {
        return buf.getInt(TILE_INDEX_PTR_OFS) + TILE_INDEX_PTR_OFS;
    }

    // TODO: consolidate with getIndexEntry
    public int tilePage(int tip)
    {
        return baseMapping.getInt(tileIndexPointer() + tip * 4) >>> 1;
    }

    public String stringFromCode(int code)
    {
        try
        {
            return codesToStrings[code];
        }
        catch (IndexOutOfBoundsException ex)
        {
            throw new RuntimeException(String.format(
                "Undefined global string code %d", code));
        }
    }

    /**
     * Returns the global string code for a given string.
     *
     * @param s     the string to look up in the GST
     * @return      the string code (range 0 -  65,535 inclusive), or -1
     *              if the string is not in the GST
     */
    public int codeFromString(String s)
    {
        return stringsToCodes.getIfAbsent(s, -1);
    }

    public GeometryFactory geometryFactory()
    {
        return geometryFactory;
    }

    public Matcher getMatcher(String query)
    {
        synchronized (matchers)
        {
            return matchers.getMatcher(query);
        }
    }

    @Override protected int getIndexEntry(int id)
    {
        return super.getIndexEntry(id) >>> 1;
    }

    @Override protected void setIndexEntry(int id, int page)
    {
        super.setIndexEntry(id, page << 1);
    }

    public int fetchTile(int tip)
    {
        return fetchBlob(tip);
    }

    /*
    public int fetchTile(int tip)
    {
        assert tip >= 0 && tip < (1 << 24) : String.format("Invalid TIP: %d", tip);
        ByteBuffer buf = baseMapping;
        int pTileIndex = tileIndexPointer();
        int p = pTileIndex + tip * 4;
        int entry = buf.getInt(p);
        int page;
        if (entry == 0)
        {
            // TODO: we need a method to download asynchronously,
            //  and re-submit the TileQueryTask; this way, we can process
            //  other tiles while we're waiting for the download to finish

            page = 0;
            assert false;
        }
        else
        {
            assert (entry & 1) == 0;
            page = entry >>> 1;
        }
        return page;
    }
     */

    public StoredFeature getFeature(ByteBuffer buf, int p)
    {
        int flags = buf.getInt(p);
        int type = (flags >> 3) & 3;
        if(type == 1)
        {
            return new StoredWay(this, buf, p);
        }
        if(type == 0)
        {
            return new StoredNode(this, buf, p);
        }
        assert type == 2;
        return new StoredRelation(this, buf, p);
    }

    public StoredWay getWay(int tip, int ptr)
    {
        int tilePage = fetchTile(tip);
        return new StoredWay(this, bufferOfPage(tilePage),offsetOfPage(tilePage) + ptr);
    }

    // TODO: create an awaitOperations() method
    @Override public void close()
    {
        if(executor != null)
        {
            // Wait for pending tasks to complete before allowing
            // Store.close() to unmap the buffers (otherwise risk of crash)

            executor.shutdown();
            try
            {
                // Log.debug("close(): Waiting for query threads to shut down...");
                // new Exception().printStackTrace();
                executor.awaitTermination(24, TimeUnit.HOURS);
            }
            catch (InterruptedException e)
            {
                // do nothing
            }
        }
        super.close();
    }

    public IntIntMap keysToCategories()
    {
        return keysToCategories;
    }

    public Map<String,Integer> indexedKeys()
    {
        Map<String,Integer> indexedKeys  =new HashMap<>(keysToCategories.size());
        keysToCategories.forEachKeyValue((k,category) ->
        {
            indexedKeys.put(codesToStrings[k], category);
        });
        return indexedKeys;
    }

    public ObjectIntMap<String> stringsToCodes()
    {
        return stringsToCodes;
    }

    public String[] codesToStrings()
    {
        return codesToStrings;
    }

    // TODO: Use only for build; does not use transaction
    //  Move to Compiler?
    public synchronized int createTile(int tip, int size)
    {
        int page = allocateBlob(size);
        int p = tileIndexPointer() + tip * 4;
        baseMapping.putInt(p, page << 1);
        return page;
    }

    /**
     * Resets the metadata section to a blank state (so it can be copied or
     * exported). This method is *never* applied to the FeatureStore's live
     * metadata, but always a copy.
     *
     * In addition to the BlobStore base implementation (which clears the
     * free-blob table and sets the total page count to zero), this method
     * clears every TIP in the tile index.
     *
     * @param buf    the buffer containing a copy of the FeatureStore metadata
     */

    @Override protected void resetMetadata(ByteBuffer buf)
    {
        super.resetMetadata(buf);

        // Reset each tile-index entry
        int pTileIndex = tileIndexPointer();
        TileIndexWalker walker = new TileIndexWalker(this);
        walker.start(Box.ofWorld());
        while(walker.next())
        {
            buf.putInt(pTileIndex + walker.tip() * 4, 0);
        }

        // reset the purgatory tile
        buf.putInt(pTileIndex, 0);
    }
}