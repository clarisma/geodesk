/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.Feature;
import com.geodesk.feature.store.FeatureConstants;

import java.nio.ByteBuffer;
import java.util.Iterator;

public abstract class TableIterator<Feature> implements Iterator<Feature>
{
    protected int tip = FeatureConstants.START_TIP;
    protected ByteBuffer foreignBuf;
    protected int pForeignTile;

    // TODO: consolidate these
    protected static final int LAST_FLAG = 1;
    protected static final int FOREIGN_FLAG = 2;
    protected static final int DIFFERENT_TILE_FLAG = 8;
}
