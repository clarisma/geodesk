package com.geodesk.feature.query;

import com.geodesk.feature.Feature;
import com.geodesk.feature.store.FeatureConstants;

import java.nio.ByteBuffer;
import java.util.Iterator;

public abstract class TableIterator<T extends Feature> implements Iterator<T>
{
    protected int tip = FeatureConstants.START_TIP;
    protected ByteBuffer foreignBuf;
    protected int pForeignTile;

    protected static final int LAST_FLAG = 1;
    protected static final int FOREIGN_FLAG = 2;
    protected static final int DIFFERENT_TILE_FLAG = 8;
}
