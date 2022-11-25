/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import org.locationtech.jts.geom.Geometry;

public abstract class SlowSpatialFilter implements Filter
{
    protected abstract boolean acceptGeometry(Geometry geom);

    @Override public boolean accept(Feature feature)
    {
        return acceptGeometry(feature.toGeometry());
    }
}
