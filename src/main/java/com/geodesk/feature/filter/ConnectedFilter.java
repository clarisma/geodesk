/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.core.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;

public class ConnectedFilter implements Filter
{
    private Feature self;
    private MutableLongSet points = new LongHashSet();
    private Bounds bounds;

    public ConnectedFilter(Feature f)
    {
        self = f;
        collectPoints(f);
        bounds = f.bounds();
    }

    public ConnectedFilter(Geometry geom)
    {
        Box bbox = new Box();
        geom.apply(new CoordinateSequenceFilter()
        {
            @Override public void filter(CoordinateSequence coords, int i)
            {
                int x = (int)Math.round(coords.getX(i));
                int y = (int)Math.round(coords.getY(i));
                points.add(XY.of(x,y));
                bbox.expandToInclude(x,y);
            }

            @Override public boolean isDone()
            {
                return false;
            }

            @Override public boolean isGeometryChanged()
            {
                return false;
            }
        });
        bounds = bbox;
    }

    @Override public Bounds bounds()
    {
        return bounds;
    }

    private void collectPoints(Feature f)
    {
        if (f instanceof Way)
        {
            // TODO: accept other implementations
            StoredWay way = (StoredWay) f;
            StoredWay.XYIterator iter = way.iterXY(0);
            while (iter.hasNext())
            {
                points.add(iter.nextXY());
            }
        }
        else if (f instanceof Relation rel)
        {
            for (Feature member : rel) collectPoints(member);
        }
        else
        {
            assert f instanceof Node;
            points.add(XY.of(f.x(), f.y()));
        }
    }

    @Override public boolean accept(Feature feature)
    {
        if(self != null && self.equals(feature)) return false;
        if(feature instanceof Way)
        {
            // TODO: accept other implementations
            StoredWay way = (StoredWay)feature;
            StoredWay.XYIterator iter = way.iterXY(0);
            while (iter.hasNext())
            {
                if(points.contains(iter.nextXY())) return true;
            }
            return false;
        }
        if (feature instanceof Relation rel)
        {
            for (Feature member : rel)
            {
                if(accept(member)) return true;
            }
            return false;
        }
        assert feature instanceof Node;
        return points.contains(XY.of(feature.x(), feature.y()));
    }
}