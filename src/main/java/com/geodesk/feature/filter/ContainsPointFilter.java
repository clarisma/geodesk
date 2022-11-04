/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.feature.*;
import com.geodesk.geom.Bounds;

// TODO

/**
 * Spatial predicate that accepts only features that contain a given point.
 */
public class ContainsPointFilter implements Filter
{
    private final Bounds bounds;
    private final int px;
    private final int py;

    @Override public Bounds bounds()
    {
        return bounds;
    }

    public ContainsPointFilter(double distance, int x, int y)
    {
        this.px = x;
        this.py = y;
        bounds = Box.atXY(x,y);
    }

    @Override public boolean accept(Feature feature)
    {
        return false;   // TODO
    }
}

