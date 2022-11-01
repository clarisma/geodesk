/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.core.Tile;
import com.geodesk.geom.Bounds;

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
    // private int currentTilePage;

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

        void init(int parentTile, Bounds bounds)
        {
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
        this.bounds = bounds;
        root.init(0, bounds);
        current = root;
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
                int pEntry = level.pChildEntries + childEntry * 4;
                int pageOrPtr = buf.getInt(pEntry);
                if((pageOrPtr & 1) != 0)
                {
                    // current tile has children
                    current = level = level.child;
                    pEntry += (pageOrPtr ^ 1);
                    // currentTilePage = buf.getInt(pBranch);
                    level.init(currentTile, bounds);
                    level.childTileMask = buf.getLong(pEntry + 4);
                    level.pChildEntries = pEntry + (level.extent==8 ? 12 : 8);
                }
                currentTip = (pEntry - tileIndexPointer()) / 4;
                return true;
            }
        }
    }
}
