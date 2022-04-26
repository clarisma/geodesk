package com.geodesk.feature.store;

import com.clarisma.common.pbf.PbfDecoder;
import com.clarisma.common.store.BlobStore;
import com.geodesk.feature.Features;
import com.geodesk.feature.filter.FilterSet;
import com.geodesk.feature.query.FilterCompiler;
import com.geodesk.geom.Bounds;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class FeatureStoreBase extends BlobStore
{
    private int minZoom;
    private int zoomSteps;
    private ObjectIntMap<String> stringsToCodes;
    private String[] codesToStrings;
    private IntIntMap keysToCategories;
    private ExecutorService executor;
    private FilterCompiler filters;
    private GeometryFactory geometryFactory;

    public static final int MAGIC = 0x1CE50D6E;  // "geodesic"
    public static final int VERSION = 1_000_000;

    private static final int MAGIC_CODE_OFS = 32;
    private static final int ZOOM_LEVELS_OFS = 40;
    private static final int TILE_INDEX_PTR_OFS = 44;
    private static final int PROPERTIES_PTR_OFS = 48;
    private static final int STRING_TABLE_PTR_OFS = 52;
    private static final int INDEX_SCHEMA_PTR_OFS = 56;

    // TODO: move to subclass, don't need full functionality
    //  for the GolTool
    @Override protected void initialize()
    {
        super.initialize();
        readStringTable();  // TODO: re-enable
        readIndexSchema();  // TODO: re-enable
        // enableQueries();
        int zoomLevels = zoomLevels();
        minZoom = ZoomLevels.minZoom(zoomLevels);
        zoomSteps = ZoomLevels.zoomSteps(zoomLevels);
    }

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

    protected int zoomLevels()
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

        MutableObjectIntMap<String> stringMap =
            new ObjectIntHashMap<>(count + (count >> 1));

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
        filters = new FilterCompiler(stringsToCodes, codesToStrings, keysToCategories);
        executor = new ForkJoinPool();// TODO: ability to set parallelism
        geometryFactory = new GeometryFactory(); // TODO
    }

    protected ExecutorService executor()
    {
        return executor;
    }

    protected int tileIndexPointer()
    {
        return baseMapping.getInt(TILE_INDEX_PTR_OFS) + TILE_INDEX_PTR_OFS;
    }

    protected int tilePage(int tip)
    {
        return baseMapping.getInt(tileIndexPointer() + tip * 4) >>> 1;
    }

    protected String stringFromCode(int code)
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

    protected int codeFromString(String s)
    {
        return stringsToCodes.get(s);
    }

    public GeometryFactory geometryFactory()
    {
        return geometryFactory;
    }

    public FilterSet getFilters(String query)
    {
        synchronized (filters)
        {
            return filters.getFilters(query);
        }
    }

    protected int fetchTile(int tip)
    {
        assert tip >= 0 && tip < (1 << 24) : String.format("Invalid TIP: %d", tip);
        ByteBuffer buf = baseMapping;
        int pTileIndex = tileIndexPointer();
        int p = pTileIndex + tip * 4;
        int entry = buf.getInt(p);
        int page;
        if (entry == 0)
        {
            // TODO: download tile (must be synchronized)
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

    public Features<?> features()
    {
        return new WorldView(this);
    }

    public Features<?> features(String filter)
    {
        return new WorldView<>(this, getFilters(filter));
    }

    public Features<?> in(Bounds bbox)
    {
        return new WorldView(this, bbox);
    }

    @Override public void close()
    {
        if(executor != null)
        {
            // Wait for pending tasks to complete before allowing
            // Store.close() to unmap the buffers (otherwise risk of crash)

            executor.shutdown();
            try
            {
                executor.awaitTermination(24, TimeUnit.HOURS);
            }
            catch (InterruptedException e)
            {
                // do nothing
            }
        }
        super.close();
    }
}