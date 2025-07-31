/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.*;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;
import com.geodesk.geom.Box;
import com.geodesk.geom.XY;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;

public class IdFilter implements Filter
{
    protected final int types;
    protected final long id;

    public IdFilter(int types, long id)
    {
        this.types = types;
        this.id = id;
    }

    @Override public int strategy()
    {
        return FilterStrategy.RESTRICTS_TYPES;
    }

    @Override public int acceptedTypes()
    {
        return types;
    }

    @Override public boolean accept(Feature feature)
    {
        return feature.id() == id;
    }
}