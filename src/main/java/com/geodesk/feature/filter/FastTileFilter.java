/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.core.Tile;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.StoredFeature;
import org.locationtech.jts.geom.Geometry;

/**
 * A shortcut filter that spatial predicates can supply for tiles that are
 * properly contained within the test geometry, in order to avoid more
 * complex topological tests. If a feature lies completely within the tile
 * (i.e. it is not multi-tile, it can be quickly accepted/rejected; for
 * multi-tile features, the original Filter is applied.
 */
// TODO: don't apply to nodes (Nodes can never be multi-tile)
public class FastTileFilter implements Filter
{
    private final boolean fastAccept;
    private final Filter slowFilter;
    private final int tileMaxX;
    private final int tileMinY;

    public FastTileFilter(int tile, boolean fastAccept, Filter slowFilter)
    {
        this.fastAccept = fastAccept;
        this.slowFilter = slowFilter;
        tileMaxX = Tile.rightX(tile);
        tileMinY = Tile.bottomY(tile);
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        // TODO: Should we explicitly check for nodes first?
        //  Quicker decision, because nodes always lie within a tile,
        //   but adds one more check for non-node features
        //   Maybe better to let the predicate return `null` instead of
        //   this filter if query only applies to nodes
        // TODO: Would be useful if `Feature` implemented `Bounds` for this
        StoredFeature sf = (StoredFeature)feature;
        if((sf.flags() & FeatureFlags.MULTITILE_FLAGS) == 0)
        {
            if(sf.minY() >= tileMinY && sf.maxX() <= tileMaxX) return fastAccept;
        }
        if(geom==null) geom=feature.toGeometry();
        return accept(feature, null);
    }
}
