package com.geodesk.feature.store;

import com.clarisma.common.store.BlobStoreChecker;
import com.geodesk.core.Box;
import com.geodesk.feature.FeatureLibrary;

import java.nio.ByteBuffer;

public class FeatureStoreChecker extends BlobStoreChecker
{
    private int zoomLevels;
    private int pIndex;

    public FeatureStoreChecker(FeatureStore store)
    {
        super(store);
    }

    @Override protected void checkMetadata()
    {
        super.checkMetadata();
        ByteBuffer buf = bufferOfPage(0);
        zoomLevels = buf.getInt(FeatureStore.ZOOM_LEVELS_OFS);
        pIndex = buf.getInt(FeatureStore.TILE_INDEX_PTR_OFS) + FeatureStore.TILE_INDEX_PTR_OFS;
            // TODO: make pointer absolute?
    }

    @Override protected void checkIndex()
    {
        // TODO: Use safer approach to walking in case
        //  index is corrupt

        ByteBuffer buf = bufferOfPage(0);
        TileIndexWalker walker = new TileIndexWalker(buf, pIndex, zoomLevels);
        walker.start(Box.ofWorld());
        while(walker.next())
        {
            int tip = walker.tip();
            int p = pIndex + tip * 4;
            int page = walker.tilePage();
            if(page != 0) useBlob(p, page);
        }

        // Purgatory
        int entry = buf.getInt(pIndex);
        if(entry != 0) useBlob(pIndex, entry >>> 1);
    }

    public static void main(String[] args)
    {
        // FeatureLibrary features = new FeatureLibrary("c:\\geodesk\\tests\\de2.gol");
        FeatureLibrary features = new FeatureLibrary("c:\\geodesk\\tests\\s2.gol");
        FeatureStoreChecker checker = new FeatureStoreChecker(features.store());
        checker.check();
        checker.reportErrors(System.out);
    }

}
