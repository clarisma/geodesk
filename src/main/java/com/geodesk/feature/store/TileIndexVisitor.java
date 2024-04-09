/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.geom.Tile;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;

public abstract class TileIndexVisitor
{
    protected Bounds bounds;
    protected ByteBuffer buf;
    protected int pos;
    protected int zoomSteps;

    public TileIndexVisitor(ByteBuffer buf, int pos, int zoomSteps, Bounds bounds)
    {
        this.buf = buf;
        this.pos = pos;
        this.zoomSteps = zoomSteps;
        this.bounds = bounds;
    }

    protected abstract void visit(int tip, int tile);

    private void visitTile(int p, int tile, int zoomSteps)
    {
        int pTilePage;
        int entry = buf.getInt(p);
        if((entry & 1) == 0)
        {
            pTilePage = p;
        }
        else
        {
            pTilePage = p + (entry ^ 1);

            // log.debug("pTilePage = {}", pTilePage);

            int step = zoomSteps & 3;
            long childTileMask = buf.getLong(pTilePage+4);
            int pChildren = pTilePage + (step==3 ? 12 : 8);
            // If step is 3, that means there are 64 child tiles,
            // which means the childTileMask takes up 8 bytes
            // instead of 4; we fetch as a long in either case,
            // since the high word will be ignored if there are
            // 4 or 16 children
            int childZoom = Tile.zoom(tile) + step;
            int extent = 1 << step;
            int childZoomSteps = zoomSteps >>> 2;

            // Now, we check which child tiles intersect the
            // query's bounding box -- depending on the zoom step
            // between the current tile and the next-higher level,
            // this is a 2x2, 4x4, or 8x8 matrix

            int tileTop = Tile.row(tile) << step;
            int tileLeft = Tile.column(tile) << step;
            int left =   Tile.columnFromXZ(bounds.minX(), childZoom);
            int right =  Tile.columnFromXZ(bounds.maxX(), childZoom);
            int top =    Tile.rowFromYZ(bounds.maxY(), childZoom);
            int bottom = Tile.rowFromYZ(bounds.minY(), childZoom);
            int startCol = Math.max(left-tileLeft, 0);
            int startRow = Math.max(top-tileTop, 0);
            int endCol = Math.min(right-tileLeft, extent-1);
            int endRow = Math.min(bottom-tileTop, extent-1);
            for(int row=startRow; row<=endRow; row++)
            {
                for(int col=startCol; col<=endCol; col++)
                {
                    int childNumber = row*extent+col;
                    if((childTileMask & (1L << childNumber)) != 0)
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
                            << (63-childNumber)) - 1;
                        // cannot shift by 64; only the lowest 5 bits count

                        // by counting how many bits are set in the
                        // mask before the bit at childNumber, we
                        // determine the position of this child's
                        // entry (This should be a very fast operation
                        // on modern CPUs)

                        int childTile = Tile.fromColumnRowZoom(
                            tileLeft+col, tileTop+row, childZoom);
                        // log.debug("Creating task for Tile {}...", Tile.toString(childTile));
                        visitTile(pChildren + childEntry * 4, childTile, childZoomSteps);
                    }
                }
            }
        }
    }

    public void visitAll()
    {
        visitTile(pos + 4, 0, zoomSteps);
    }
}
