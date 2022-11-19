/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.feature.match.TypeBits;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * An interface for classes that select the features to be returned by a query.
 */
public interface Filter
{
    /**
     * Returns zero or more bit flags specified in
     * {@link com.geodesk.feature.filter.FilterStrategy}
     * that help the Query Engine optimize the performance of this Filter.
     *
     * @return a bit set of strategy flags
     */
    default int strategy() { return 0; }

    /**
     * Checks whether the given feature should be included in the query results.
     *
     * @param feature   the feature to check
     * @return          `true` if this feature should be included in the results
     */
    default boolean accept(Feature feature)
    {
        return accept(feature, feature.toGeometry());
    }

    /**
     * Checks whether the given feature should be included in the query results.
     * If `stategy()` includes `NEEDS_GEOMETRY`, `geom` must not be `null`.
     *
     * @param feature   the feature to check
     * @param geom      the feature's geometry
     * @return          `true` if this feature should be included in the results
     */
    default boolean accept(Feature feature, Geometry geom)
    {
        return accept(feature);
    }

    default boolean acceptGeometry(Geometry geom)
    {
        return true;
    }

    /**
     * Returns the Filter that should be used for the given tile.
     * This allows a Filter to accept all features within a certain tile,
     * reject a tile entirely, or substitute itself with a cheaper filter.
     * This method will only be called if `strategy()` includes
     * `FAST_TILE_FILTER`.
     *
     * Note that to signal that all features should be accepted, this method
     * returns `null` (i.e. nothing is filtered out); to reject the tile
     * entirely, it must return `FalseFilter.INSTANCE`. It can return `this`
     * to indicate that no shortcut filter is available for the given tile.
     *
     * @param tileNumber        the tile number
     * @param tileGeometry      the tile polygon (an axis-aligned square)
     * @return                  the filter to use for this tile
     *
     */
    default Filter filterForTile(int tileNumber, Polygon tileGeometry) { return this; }

    /**
     * The maximum bounding box in which acceptable candidates can be found.
     *
     * @return a bounding box, or`null` if the filter does not use
     *   the spatial index
     */
    default Bounds bounds() { return null; }

    default int acceptedTypes() { return TypeBits.ALL; }
}
