/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.clarisma.common.util.Log;
import com.geodesk.core.Heading;
import com.geodesk.core.Tile;
import com.geodesk.feature.Filter;
import com.geodesk.feature.filter.FalseFilter;
import com.geodesk.feature.filter.FilterStrategy;
import com.geodesk.geom.Bounds;
import com.geodesk.util.GeometryBuilder;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

/**
 * A class that traverses the Tile Index Tree of a FeatureStore in an
 * iterator-like fashion, returning all tiles that intersect a given
 * bounding box.
 */
// TODO: no need to create top level (leaf)
public class TileIndexWalker
{
    private Bounds bounds;
    private final ByteBuffer buf;
    private final Level root;
    private Level current;
    private int currentTile;
    private int currentTip;
    private Filter filter;
    private int northwestFlags;
    private MutableIntSet acceptedTiles;
    private boolean tileBasedAcceleration;

    // TODO: could the col/rows be shorts? Performance impact?
    private static class Level
    {
        Level parent;
        Level child;
        long childTileMask;
        int pChildEntries;
        int topLeftChildTile;
        int extent;     // TODO: do we need to store this?
        int startCol;
        int startRow;   // TODO: could drop this
        int endCol;
        int endRow;
        int currentCol;
        int currentRow;
        Filter filter;

        void init(int parentTile, Bounds bounds, Filter filter)
        {
            this.filter = filter;
            int zoom = Tile.zoom(topLeftChildTile);
            int step = zoom - Tile.zoom(parentTile);
            // int extent = 1 << step;     // TODO: could take it from Level object
            int tileTop = Tile.row(parentTile) << step;
            int tileLeft = Tile.column(parentTile) << step;
            topLeftChildTile = Tile.fromColumnRowZoom(tileLeft, tileTop, zoom);
            int left =   Tile.columnFromXZ(bounds.minX(), zoom);
            int right =  Tile.columnFromXZ(bounds.maxX(), zoom);
            int top =    Tile.rowFromYZ(bounds.maxY(), zoom);
            int bottom = Tile.rowFromYZ(bounds.minY(), zoom);
            startCol = Math.max(left-tileLeft, 0);
            startRow = Math.max(top-tileTop, 0);
            endCol = Math.min(right-tileLeft, extent-1);
            endRow = Math.min(bottom-tileTop, extent-1);
            currentCol = startCol - 1;
            currentRow = startRow;
        }
    }

    public TileIndexWalker(ByteBuffer buf, int pTileIndex, int zoomLevels)
    {
        this.buf = buf;

        current = root = new Level();
        Level level = root;
        int zoom = -1;
        for(;;)
        {
            int step = Integer.numberOfTrailingZeros(zoomLevels) + 1;
            zoom += step;
            level.topLeftChildTile = Tile.fromColumnRowZoom(0,0,zoom);
            level.extent = 1 << step;
            zoomLevels >>>= step;
            if(zoomLevels == 0) break;
            Level child = new Level();
            level.child = child;
            child.parent = level;
            level = child;
        }

        // initialize the root

        level = root;
        level.extent >>>= 1;    // fix the root extent
        level.childTileMask = ~0;      // root tile raster is always dense
        level.pChildEntries = pTileIndex + 4;   // skip purgatory tile
    }

    public TileIndexWalker(FeatureStore store)
    {
        this(store.baseMapping(), store.tileIndexPointer(), store.zoomLevels());
    }

    public void start(Bounds bounds)
    {
        start(bounds, null);
    }

    public void start(Bounds bounds, Filter filter)
    {
        this.bounds = bounds;
        this.filter = filter;
        root.init(0, bounds, filter);
        current = root;
        acceptedTiles = null;
        if(filter != null)
        {
            int strategy = filter.strategy();
            if((strategy & FilterStrategy.FAST_TILE_FILTER) != 0)
            {
                tileBasedAcceleration = true;
                if ((strategy & FilterStrategy.STRICT_BBOX) == 0)
                {
                    acceptedTiles = new IntHashSet();
                }
            }
        }
    }

    protected int tileIndexPointer()
    {
        return root.pChildEntries - 4;      // TODO: maybe set ptr one word ahead?
    }

    public int tile()
    {
        return currentTile;
    }

    public int tip()
    {
        return currentTip;
    }

    public Filter filter()
    {
        return filter;
    }

    public int northwestFlags()
    {
        return northwestFlags;
    }

    public int tilePage()
    {
        int p = tileIndexPointer() + currentTip * 4;
        int entry = buf.getInt(p);
        assert (entry & 1) == 0;
        return entry == 0? 0 : (entry >>> 1);
    }

    public boolean next()
    {
        Level level = current;
        long childTileMask = level.childTileMask;
        for(;;)
        {
            level.currentCol++;
            if (level.currentCol > level.endCol)
            {
                level.currentRow++;
                if (level.currentRow > level.endRow)
                {
                    // we are done with this level
                    current = level = level.parent;
                    if(level == null)
                    {
                        // We've completed the root; we are done
                        return false;
                    }
                    childTileMask = level.childTileMask;
                    continue;
                }
                else
                {
                    level.currentCol = level.startCol;
                }
            }
            int childNumber = level.currentRow * level.extent + level.currentCol;
            if ((childTileMask & (1L << childNumber)) != 0)
            {
                // If the bit in the childTileMask is set,
                // this means that there is actually a tile
                // at this cell in the matrix
                // In the tile index, empty cells are skipped;
                // if we have a 4x4 matrix, and the mask bits
                // are 0b0000_0000_0011_0100, this means the
                // record is laid out like this:
                //
                // [parent tile]
                // [childTileMask]
                // [child at row0, col2]
                // [child at row1, col0]
                // [child at row1, col1]

                int childEntry = Long.bitCount(childTileMask
                    << (63 - childNumber)) - 1;
                // cannot shift by 64; only the lowest 5 bits count
                // TODO: could avoid -1 by setting pChildEntries one word earlier

                // by counting how many bits are set in the
                // mask before the bit at childNumber, we
                // determine the position of this child's
                // entry (This should be a very fast operation
                // on modern CPUs)

                currentTile = Tile.relative(level.topLeftChildTile,
                    level.currentCol, level.currentRow);
                // Log.debug("TIW: Current tile %s, Filter = %s", Tile.toString(currentTile), filter);

                if(tileBasedAcceleration)
                {
                    // If the Filter allows for tile-based acceleration (rejecting
                    // a tile, waiving the filter, or substituting the filter for
                    // a cheaper one), create a polygon for the current tile and
                    // check with the Filter

                    // We need to check the strategy flags of each filter (not just the
                    // original one), because a shortcut filter may not perform further
                    // substitution -- this saves us from needlessly checking

                    // TODO: check current tile bounds to see if they completely enclose
                    //  the query bbox; we can skip the disjoint/containsProperly test.
                    //   which is expensive for this case

                    if(level.filter != null && (level.filter.strategy() & FilterStrategy.FAST_TILE_FILTER) != 0)
                    {
                        filter = level.filter.filterForTile(currentTile, Tile.polygon(currentTile));
                        if (filter == FalseFilter.INSTANCE) continue;
                    }
                    if (acceptedTiles != null)
                    {
                        northwestFlags =
                            (acceptedTiles.contains(Tile.neighbor(currentTile, Heading.NORTH)) ?
                                FeatureFlags.MULTITILE_NORTH : 0) |
                            (acceptedTiles.contains(Tile.neighbor(currentTile, Heading.WEST)) ?
                                FeatureFlags.MULTITILE_WEST : 0);
                        acceptedTiles.add(currentTile);
                    }
                    else
                    {
                        // If we're not tracking accepted NW tiles (for filters that
                        // use a strict bbox), pretend that NW tiles exist
                        // If a feature extends into a N/W tile, the query bbox must
                        // extend into the N/W tile as well, else it cannot be fully
                        // within the bbox
                        // (For simplicity, we could track tiles for strict-bbox filters
                        // as well; in that case, remove "tileBasedAcceleration", use
                        // only "acceptedTiles != null" as switch between NW-tile method

                        northwestFlags = FeatureFlags.MULTITILE_NORTH | FeatureFlags.MULTITILE_WEST;
                    }
                }
                else
                {
                    // If we're processing a dense set of tiles, calculate the
                    // northwestFlags based on query bbox
                    // TODO: There's probably a cheaper way to calculate this

                    northwestFlags =
                        ((bounds.maxY() > Tile.topY(currentTile)) ?
                            FeatureFlags.MULTITILE_NORTH : 0) |
                        ((bounds.minX() < Tile.leftX(currentTile)) ?
                            FeatureFlags.MULTITILE_WEST : 0);
                }
                int pEntry = level.pChildEntries + childEntry * 4;
                int pageOrPtr = buf.getInt(pEntry);
                if((pageOrPtr & 1) != 0)
                {
                    // current tile has children: prepare to move up to the
                    // next level in the tile tree

                    current = level = level.child;
                    pEntry += (pageOrPtr ^ 1);
                    level.init(currentTile, bounds, filter);
                    level.childTileMask = buf.getLong(pEntry + 4);
                    level.pChildEntries = pEntry + (level.extent==8 ? 12 : 8);
                }
                currentTip = (pEntry - tileIndexPointer()) / 4;
                return true;
            }
        }
    }
}
