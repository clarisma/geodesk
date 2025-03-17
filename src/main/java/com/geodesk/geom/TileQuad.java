/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

// TOOD: decide whether to check for validity

// TODO: move to gol-tool, limited use in core

/**
 * Methods for dealing with numeric values that represent Tile Quads, a
 * collection of one to four tiles that is at most 2 tiles wide and 2 tiles tall,
 * at a specific zoom level from 0 to 12.
 *   
 * A "normalized" quad a) has at least one tile and b) does not have tiles in its 
 * eastern/southern quadrants unless it also has tiles in its western/northern 
 * quadrants. Most of the methods of this class expect quads to be in normalized
 * form, or their results will be undefined.
 *
 * Tile Quads are represented as int values, which are compatible with Tile
 * numbers. The 4 most significant bits of a Tile number are always zero.
 * A Tile Quad uses these 4 bits to indicate which tiles in a quad are in use.
 * 
 *  0 --   1 X-   2 -X   3 XX   4 --   5 X-   6 -X   7 XX 
 *    --     --     --     --     X-     X-     X-     X-
 *
 *  8 --   9 X-  10 -X  11 XX  12 --  13 X-  14 -X  15 XX 
 *    -X     -X     -X     -X     XX     XX     XX     XX
 *
 * @hidden
 */
public class TileQuad 
{
	public static final int NW = 1 << 28;
	public static final int NE = 1 << 29;
	public static final int SW = 1 << 30;
	public static final int SE = 1 << 31;
	
	public static final int INVALID = -1;
	public static final int OVERSIZED = NW | NE | SW | SE;
	public static final int ROOT = NW;
	
	// not needed, this is simply NW at zoom level 0
	// public static final int OVERSIZED = NW | NE | SW | SE;
	
	// These are used only for relative quads

	private static final int RELATIVE_OVERSIZED = 64;
	private static final int RELATIVE_STARTS_WEST_BIT = 4;
	private static final int RELATIVE_STARTS_NORTH_BIT = 5;
	private static final int RELATIVE_STARTS_WEST = 1 << RELATIVE_STARTS_WEST_BIT;
	private static final int RELATIVE_STARTS_NORTH = 1 << RELATIVE_STARTS_NORTH_BIT;

	// used only for sparse sibling locators:

	private static final int NW_SIBLING = 1;
	private static final int NE_SIBLING = 2;
	private static final int SW_SIBLING = 4;
	private static final int SE_SIBLING = 8;

	// used only for dense parent locators:
	
	private static final int RELATIVE_EXTENDS_EAST = (1 << 6);
	private static final int RELATIVE_EXTENDS_SOUTH = (1 << 7);

	// public final static int SIBLING_SELF = 1;
	// public final static int SIBLING_SELF = 1;
	
	public static final int of(int... tiles)
	{
		int quad = tiles[0] | NW;
		for(int i=1; i<tiles.length; i++) quad = addQuad(quad, tiles[i] | NW);
		return quad;
	}
	
	public static final int zoom(int quad)
	{
		return Tile.zoom(quad);
	}
	
	public static void forEach(int quad, IntProcedure consumer)
	{
		if(quad == INVALID) return;
		assert quad != OVERSIZED;
		int nwTile = quad & 0x0fff_ffff;
		if((quad & NW) != 0) consumer.accept(nwTile);
		if((quad & NE) != 0) consumer.accept(nwTile+1);
		if((quad & SW) != 0) consumer.accept(nwTile + (1 << 12));
		if((quad & SE) != 0) consumer.accept(nwTile + (1 << 12) + 1);
	}

	public static int fromSingleTile(int tile)
	{
		return tile | NW;
	}
	
	public static int fromSingleTile(String tile)
	{
		return Tile.fromString(tile) | NW;
	}
	
	/**
	 * Reconstitutes a quad from an 8-bit locator, relative to a tile that must be a 
	 * child tile of the quad. The resultant tile is always dense, i.e. its tiles form
	 * a rectangular shape, and is guaranteed to be normalized. 
	 * 
	 * @param loc			the locator
	 * @param originTile	a valid tile
	 * @return
	 */
	public static int fromDenseParentLocator(byte loc, int originTile)
	{
		if(loc == INVALID) return INVALID;
		// if(loc == RELATIVE_OVERSIZED) return ROOT;
		int zoom = loc & 15;
		assert zoom <= 12: String.format("Illegal locator: %d", loc);
		int startTile = Tile.zoomedOut(originTile, zoom);
		int col = Tile.column(startTile) - ((loc >> RELATIVE_STARTS_WEST_BIT) & 1);
		int row = Tile.row(startTile) - ((loc >> RELATIVE_STARTS_NORTH_BIT) & 1);
		int quad = Tile.fromColumnRowZoom(col, row, zoom) | NW;
		if((loc & RELATIVE_EXTENDS_EAST) != 0) quad |= NE;
		if((loc & RELATIVE_EXTENDS_SOUTH) != 0) quad |= (quad & (NW | NE)) << 2;
		return quad;
	}

	/**
	 * Reconstitutes a quad from a 6-bit locator, relative to a tile that must lie 
	 * within the bounds of the quad (but does not have to be a member of the quad).
	 * The resulting quad is sparse and is only guaranteed to be normalized if
	 * the locator itself is normalized.
	 * 
	 * @param loc			the locator
	 * @param originTile	a valid tile
	 * @return
	 */
	public static int fromSparseSiblingLocator(byte loc, int originTile)
	{
		if(loc == INVALID) return INVALID;
		if(loc == RELATIVE_OVERSIZED) return ROOT;
		int zoom = Tile.zoom(originTile);
		int col = Tile.column(originTile) - ((loc >> RELATIVE_STARTS_WEST_BIT) & 1);
		int row = Tile.row(originTile) - ((loc >> RELATIVE_STARTS_NORTH_BIT) & 1);
		return Tile.fromColumnRowZoom(col, row, zoom) | ((loc & 15) << 28);
	}

	/**
	 * Returns an 8-bit locator, which describes a quad relative to a child tile
	 * that is covered by it. The quad and the tile must be valid, the quad must
	 * be normalized, and its bounds must include the child tile, or else the
	 * result will be undefined. 
	 *   
	 * @param parentQuad		
	 * @param childTile
	 * @return
	 */
	public static byte toDenseParentLocator(int parentQuad, int childTile)
	{
		if(parentQuad == INVALID) return INVALID;
		int parentZoom = zoom(parentQuad); 
		assert parentZoom <= Tile.zoom(childTile):
			String.format(
				"Cannot create a locator, since %s is not a parent " +
				"quad of %s", TileQuad.toString(parentQuad), 
				Tile.toString(childTile));
		if(parentZoom > Tile.zoom(childTile)) return INVALID;
		// if(parentQuad == OVERSIZED) return RELATIVE_OVERSIZED;
		int startTile = Tile.zoomedOut(childTile, parentZoom);
		byte locator = (byte)parentZoom;
		int parentCol = Tile.column(parentQuad);
		int startCol = Tile.column(startTile);
		int parentRow = Tile.row(parentQuad);
		int startRow = Tile.row(startTile);

		assert coversQuad(parentQuad, childTile | NW):
			String.format(
				"Cannot create a locator, since %s is not a parent " +
				"quad of %s", TileQuad.toString(parentQuad), 
				Tile.toString(childTile));
		
		if(parentCol < startCol) locator |= RELATIVE_STARTS_WEST;
		if(parentRow < startRow) locator |= RELATIVE_STARTS_NORTH;
		locator |= ((parentQuad & NE) >>> 23) | ((parentQuad & SE) >>> 25);
		locator |= ((parentQuad & SW) >>> 23) | ((parentQuad & SE) >>> 24);
		return locator;
	}
	
	/**
	 * Returns a 6-bit locator, which describes a quad relative to a tile
	 * contained within its bounds. The quad and the tile must be valid, the
	 * quad must be normalized, and its bounds must include the tile, or
	 * else the result will be undefined.
	 *   
	 * @param quad
	 * @param startTile
	 * @return
	 */
	public static byte toSparseSiblingLocator(int quad, int startTile)
	{
		if(quad == OVERSIZED) return RELATIVE_OVERSIZED;
		if(quad == ROOT && startTile != 0) return RELATIVE_OVERSIZED;
		byte locator = 0;
		int quadCol = Tile.column(quad);
		int startCol = Tile.column(startTile);
		int quadRow = Tile.row(quad);
		int startRow = Tile.row(startTile);

		// TODO: check if startTile is in the quad
		assert zoom(quad) == Tile.zoom(startTile) && 
			quadCol-startCol <= 1 &&
			quadCol-startCol >= -1 &&
			quadRow-startRow <= 1 &&
			quadRow-startRow >= -1;
		
		if(quadCol < startCol) locator |= RELATIVE_STARTS_WEST;
		if(quadRow < startRow) locator |= RELATIVE_STARTS_NORTH;
		return (byte)(locator | (quad >>> 28));
	}
	
	
	
	// quad must be in normalized form
	public static int zoomedOut(int quad, int zoom)
	{
		if(quad == INVALID) return INVALID;
		int currentZoom = zoom(quad);
		if(currentZoom <= zoom)	return (currentZoom == zoom) ? quad : INVALID;
		int delta = currentZoom-zoom;
		int left = Tile.column(quad);
		int right = left+1;
		int top = Tile.row(quad);
		int bottom = top+1;
		left >>= delta;
		right >>= delta;
		top >>= delta;
		bottom >>= delta;
		
		// We don't worry about overflow, because a valid quad with E or S bits
		// set must start at (max-1); if right/bottom are out of range, there
		// should be no bits set so the overflow has no effect
		
		int xShift = 1-(right-left);			// 1 if left/right are now the same, otherwise 0
		int yShift = (1-(bottom-top)) * 2;	// 2 if top/bottom are now the same, otherwise 0
		int newQuad = Tile.fromColumnRowZoom(left, top, zoom);
		newQuad |= quad & NW;		// NW bit is always preserved
		int ne = (quad & NE) >>> xShift;
		int sw = (quad & SW) >>> yShift;
		int se = (quad & SE) >>> (xShift + yShift);
		return newQuad | ne | sw | se;
	}
	
	/**
	 * Determine the minimum zoom level at which a span of columns (or rows) 
	 * can be represented as a quad. 
	 * 
	 * @param min	left column (or top row) at zoom level 16
	 * @param max   right column (or bottom row) at zoom level 16
	 * @return
	 */
	private static final int BASE_ZOOM = 16;
	
	private static int commonZoom(int min, int max)
	{
		int w = max - min;
		// we fiddle with the bits to make things work at lowest zoom level:
		// if w=0 and min is even, we make sure at least one bit is set
		int modW = w; // w ^ (min ^ ~w) & 1;
		int zoom = BASE_ZOOM - 31 + Integer.numberOfLeadingZeros(modW);
		int shift = BASE_ZOOM - zoom;
		int mask = (1 << shift) - 1;
		int offset = min & mask;
		return zoom - ((w + offset) >> (shift+1));
	}
	
	private static int rebase(int quad, int left, int top)
	{
		int col = Tile.column(quad);
		int row = Tile.row(quad);
		int xShift = col-left;
		assert xShift >= -1 && xShift <= 1: String.format("Cannot rebase %s to %d/%d", 
			toString(quad), left, top);
		int yShift = row-top;
		assert yShift >= -1 && yShift <= 1;
		int shift = xShift + yShift * 2 + 3;
		int tileBits = (((quad & 0xf000_0000) >>> 3) << shift) & 0xf000_0000;
		return Tile.fromColumnRowZoom(left, top, zoom(quad)) | tileBits;
	}
	
	/**
	 * Returns a tile quad in its "normalized" form. If the quad 
	 * has tiles in the eastern, but not western quadrants and/or 
	 * tiles in the southern, but not the northern quadrants, its 
	 * column and/or row are decreased by one, and the tile bit 
	 * pattern is adjusted accordingly. Otherwise, the quad is 
	 * returned unchanged.
	 *
	 * The quad must be valid and have at least one tile,
	 * or the result will be undefined.
	 * 
	 * @param quad
	 * @return
	 */
	public static int normalize(int quad)
	{
		int col = Tile.column(quad);
		int row = Tile.row(quad);
		int xShift = (((quad >>> 28) | (quad >>> 30)) & 1) ^ 1;
		int yShift = (((quad >>> 28) | (quad >>> 29)) & 1) ^ 1;
		int tileBits = (quad & 0xf000_0000) >>> (yShift * 2 + xShift);
		return Tile.fromColumnRowZoom(col+xShift, row+yShift, zoom(quad)) | tileBits;
	}

	public static boolean isNormalized(int quad)
	{
		return quad == normalize(quad);
	}
	
	
	/**
	 * Adds two quads together and returns the resulting quad.
	 * 
	 * @param quadA		a valid, normalized quad
	 * @param quadB		a valid, normalized quad
	 * 
	 * @return a normalized quad that covers both quads 
	 */
	public static int addQuad(int quadA, int quadB)
	{
		assert quadA != -1;
		assert quadB != -1 && quadB != 0;
		// If both quads have same zoom and origin
		if((quadA & 0x0fff_ffff) == (quadB & 0x0fff_ffff)) return quadA | quadB;
		if(quadA == 0) return quadB;
		int zoomA = zoom(quadA);
		int zoomB = zoom(quadB);
		int zoomDeltaA = BASE_ZOOM - zoomA;
		int zoomDeltaB = BASE_ZOOM - zoomB;
		
		int leftA = Tile.column(quadA);
		int leftB = Tile.column(quadB);
		int topA = Tile.row(quadA);
		int topB = Tile.row(quadB);
		int rightA = leftA + (((quadA >> 29) | (quadA >> 31)) & 1);
		int rightB = leftB + (((quadB >> 29) | (quadB >> 31)) & 1);
		int bottomA = topA + (((quadA >> 30) | (quadA >> 31)) & 1);
		int bottomB = topB + (((quadB >> 30) | (quadB >> 31)) & 1);
		
		leftA <<= zoomDeltaA;
		leftB <<= zoomDeltaB;
		topA <<= zoomDeltaA;
		topB <<= zoomDeltaB;
		// is this needed?
		rightA = ((rightA+1) << zoomDeltaA) - 1; 
		rightB = ((rightB+1) << zoomDeltaB) - 1; 
		bottomA = ((bottomA+1) << zoomDeltaA) - 1; 
		bottomB = ((bottomB+1) << zoomDeltaB) - 1;
		
		int left = Math.min(leftA, leftB);
		int right = Math.max(rightA, rightB);
		int top = Math.min(topA, topB);
		int bottom = Math.max(bottomA, bottomB);
		
		int minOriginalMinZoom = Math.min(zoomA, zoomB); 
		int zoom = Math.min(commonZoom(left,right), commonZoom(top,bottom));
		zoom = Math.min(zoom, minOriginalMinZoom);
		// assert zoom <= zoomA && zoom <= zoomB;
		quadA = zoomedOut(quadA, zoom);
		quadB = zoomedOut(quadB, zoom);
		left >>= BASE_ZOOM-zoom;
		top >>= BASE_ZOOM-zoom;
		return rebase(quadA, left, top) | rebase(quadB, left, top);
	}
	
	/**
	 * Adds a tile to the given quad and returns the resulting quad. The quad
	 * and the tile must be valid, and the quad must be normalized, or the
	 * result will be undefined.
	 * 
	 * @param quad	a valid, normalized quad
	 * @param tile	a valid tile
	 * @return a normalized quad that covers the original quad and the tile
	 */
	public static int addTile(int quad, int tile)
	{
		return addQuad(quad, tile | NW);
	}
	
	public static int addPoint(int quad, int x, int y, int zoom)
	{
		return addTile(quad, Tile.fromXYZ(x, y, zoom));
	}

	// TODO: simplify this?
	public static int addLineSegment(int quad, double startX, double startY, double endX, double endY, int zoom)
	{
		int startTile = Tile.fromXYZ(startX, startY, zoom);
		int endTile = Tile.fromXYZ(endX, endY, zoom);
		quad = addTile(quad, startTile);
		
		// TODO: fails for zoom level 0; make result long
		long tileSize = Tile.sizeAtZoom(zoom);
		
		int row = Tile.row(startTile);
		int col = Tile.column(startTile);
		double xDelta = endX-startX;
		double yDelta = endY-startY;
		for(;;)
		{
			int tile = Tile.fromColumnRowZoom(col, row, zoom);
			quad = addTile(quad, tile);
			if(tile == endTile) break;
			double left = (double)Tile.leftX(tile) - 0.5;
			double right = left + tileSize;
			double bottom = (double)Tile.bottomY(tile) - 0.5;
			double top = bottom + tileSize;
			if(endY > startY)
			{
				double x2 = startX + xDelta * (top-startY) / yDelta;
				
				// TODO: handle edge case of corner, 
				// must change col also?
				if(x2 >= left && x2 <= right)
				{
					row--;
					continue;
				}
			}
			else if(endY < startY)
			{
				double x2 = startX + xDelta * (bottom-startY) / yDelta;
				if(x2 >= left && x2 <= right)
				{
					row++;
					continue;
				}
			}
			if(endX > startX)
			{
				col++;
			}
			else
			{
				col--;
			}
		}
		return quad;
	}
	
	static private final byte[] DENSIFIED =
	{
		0,	// 0 = empty
		1,	// 1 = NW
		3,	// 2 = NE
		3,	// 3 = NW + NE
		5,	// 4 = SW
		5,  // 5 = NW + SW
		15, // 6 = SW + NE
		15,	// 7 = NW + SW + NE
		15,	// 8 = SE 
		15,	// 9 = NW + SE
		15,	// 10 = NE + SE
		15,	// 11 = NW + NE + SE
		15,	// 12 = SW + SE
		15,	// 13 = NW + SW + SE
		15,	// 14 = NE + SW + SE
		15	// 15 = full quad
	};
	
	static private final byte[] TILE_COUNTS =
	{
		0,	// 0 = empty
		1,	// 1 = NW
		1,	// 2 = NE
		2,	// 3 = NW + NE
		1,	// 4 = SW
		2,  // 5 = NW + SW
		2, // 6 = SW + NE
		3,	// 7 = NW + SW + NE
		1,	// 8 = SE 
		2,	// 9 = NW + SE
		2,	// 10 = NE + SE
		3,	// 11 = NW + NE + SE
		2,	// 12 = SW + SE
		3,	// 13 = NW + SW + SE
		3,	// 14 = NE + SW + SE
		4	// 15 = full quad
	};
		
	
	/**
	 * Returns a dense version of a quad. "Dense" means the quad contains a
	 * rectangular shape of tiles, or is empty. If the quad is not normalized,
	 * the northern/western tile bits will be set if only the southern/eastern
	 * tile bits are set.
	 * 
	 * @param quad   a valid, normalized quad
	 * @return
	 */
	public static int dense(int quad)
	{
		int flags = quad >>> 28;
		return (quad & 0x0fff_ffff) | ((DENSIFIED[flags]) << 28); 
	}
	
	/**
	 * Returns the number of tiles in the quad.
	 * 
	 * @param quad
	 * @return the number of tiles (0 to 4)
	 */
	public static int tileCount(int quad)
	{
		int flags = quad >>> 28;
		return TILE_COUNTS[flags]; 
	}
	
	public static MutableIntList toTileList(int quad)
	{
		if(quad==INVALID) return null;
		MutableIntList list = new IntArrayList(tileCount(quad));
		forEach(quad, tile -> list.add(tile));
		return list;
	}
	
	public static String quadChar(int quad)
	{
		switch(quad & 0xf000_0000)
		{
		case NW:			return "\u2598";
		case NE:			return "\u259D";
		case SW:			return "\u2596";
		case SE:			return "\u2597";
		case NW | NE:		return "\u2580";
		case SW | SE:		return "\u2584";
		case NW | SW:		return "\u258C  ";
		case NE | SE:		return "\u2590";
		case NW | NE | SW:	return "\u259B";
		case NW | NE | SE:	return "\u259C";
		case NE | SW | SE:	return "\u259F";
		case NW | SW | SE:	return "\u2599";
		case NW | SE:		return "\u259A";
		case NE | SW:		return "\u259E";
		case NW | NE | SW | SE:	return "\u2588";
		}
		return "\u25A2";
	}
	
	public static String toString(int quad)
	{
		return Tile.toString(quad) + '\u2595' + quadChar(quad) + '\u258F';
	}
	
	public static String toListString(int quad)
	{
		if(quad==INVALID) return "[invalid]";
		MutableIntList list = toTileList(quad);
		StringBuilder buf = new StringBuilder();
		buf.append('[');
		for(int i=0; i<list.size(); i++)
		{
			if(i>0) buf.append(',');
			buf.append(Tile.toString(list.get(i)));
		}
		buf.append(']');
		return buf.toString();
	}
	
	public static int width(int quad)
	{
		int west = ((quad >>> 28) | (quad >>> 30)) & 1;
		int east = ((quad >>> 29) | (quad >>> 31)) & 1;
		return west + east;
	}
	
	public static int height(int quad)
	{
		int north = ((quad >>> 28) | (quad >>> 29)) & 1;
		int south = ((quad >>> 30) | (quad >>> 31)) & 1;
		return north + south;
	}
	
	public static int northWestTile(int quad)
	{
		return quad & 0x0fff_ffff;
	}
	
	/**
	 * Subtracts one quad from another. Both quads must be at
	 * the same zoom level. The result will be in normalized
	 * form (but may be empty).
	 * 
	 * @param quadA
	 * @param quadB
	 * @return
	 */
	public static int subtractQuad(int quadA, int quadB)
	{
		int zoom = zoom(quadA);
		assert zoom == zoom(quadB): String.format("Zoom levels must match %d vs. %d",
			zoom, zoom(quadB));
		int col = Tile.column(quadA);
		int row = Tile.row(quadA);
		int xDelta = col - Tile.column(quadB);
		if(xDelta > 1 || xDelta < -1) return quadA;
		int yDelta = row - Tile.row(quadB);
		if(yDelta > 1 || yDelta < -1) return quadA;
		quadB = rebase(quadB, col, row);
		int tileBits = (quadA & 0xf000_0000) & ~quadB;
		return normalize((quadA & 0x0fff_ffff) | tileBits);
	}

	public static int subtractTile(int quad, int tile)
	{
		return subtractQuad(quad, TileQuad.fromSingleTile(tile));
	}
	
	public static boolean coversQuad(int quadA, int quadB)
	{
		int zoomA = zoom(quadA);
		int zoomB = zoom(quadB);
		if(zoomA != zoomB)
		{
			if(zoomB > zoomA)
			{
				quadB = zoomedOut(quadB, zoomA);
			}
			else
			{
				if(zoomA-zoomB > 1) return false;
				quadA = zoomedOut(quadA, zoomB);
			}
		}
		int col = Tile.column(quadA);
		int row = Tile.row(quadA);
		int xDelta = col - Tile.column(quadB);
		if(xDelta > 1 || xDelta < -1) return false;
		int yDelta = row - Tile.row(quadB);
		if(yDelta > 1 || yDelta < -1) return false;
		quadB = rebase(quadB, col, row);
		int tileBitsA = quadA >>> 28;
		int tileBitsB = quadB >>> 28;
		if(tileBitsB == 0) return false;
		return (tileBitsA & tileBitsB) == tileBitsB;
	}
	
	public static boolean coversTile(int quadA, int tileB)
	{
		return coversQuad(quadA, tileB | NW);
	}

	public static boolean containsTile(int quadA, int tileB)
	{
		if(zoom(quadA) != Tile.zoom(tileB)) return false;
		return coversQuad(quadA, tileB | NW);
	}

	// TODO: at zoom 0, can only have one tile!
	public static boolean isValid(int quad)
	{
		return Tile.isValid(quad & 0x0fff_ffff);
	}
	
	public static TileIterator iterator(int quad)
	{
		return new TileIterator(quad);
	}
	
	private static class TileIterator implements IntIterator
	{
		private int quad;
		private int pos = -1;

		public TileIterator(int quad)
		{
			this.quad = quad;
			moveToNext();
		}
		
		private void moveToNext()
		{
			for(;;)
			{
				pos++;
				if(pos > 3) break;
				if((quad & (1 << (pos + 28))) != 0) break;
			}
		}

		public int next() 
		{
			int tile = (quad & 0x0fff_ffff) + (pos & 1) + ((pos & 2) << 11);
			moveToNext();
			return tile;
		}

		public boolean hasNext() 
		{
			return pos < 4;
		}
		
	}
	
	static final long BLACK_TILES = 
		0b01011010_01010011_01011010_01010000_00110011_00010011_00010010_00010000l;
	
	public static int blackTile(int quad)
	{
		int topLeftOnWhite = (quad & 1) ^ ((quad >> 12) & 1);
		int bitPos = (topLeftOnWhite << 5) + ((quad >>> 28) << 1);
		int shifts = (int)(BLACK_TILES >>> bitPos) & 3;
		// return (quad & 0x0fff_ffff) + ((shifts & 2) << 11) + (shifts & 1);
		return Tile.fromColumnRowZoom(
			Tile.column(quad) + (shifts & 1),
			Tile.row(quad) + (shifts >> 1),
			Tile.zoom(quad));
	}
}
