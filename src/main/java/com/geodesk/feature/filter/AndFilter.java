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

public class AndFilter implements Filter
{
    private final Filter a;
    private final Filter b;

    public AndFilter(Filter a, Filter b)
    {
        this.a = a;
        this.b = b;
    }

    @Override public boolean accept(Feature feature)
    {
        return a.accept(feature) && b.accept(feature);
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return a.acceptGeometry(geom) && b.acceptGeometry(geom);
    }
}
