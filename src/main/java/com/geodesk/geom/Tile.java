/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.feature.store.BoxCoordinateSequence;
import com.geodesk.util.GeometryBuilder;
import org.locationtech.jts.geom.Envelope;

import org.locationtech.jts.geom.Polygon;


public class Tile 
{
	public static int row(int tile)
	{
		return (tile >> 12) & 0xfff;
	}
	
	public static int column(int tile)
	{
		return tile & 0xfff;
	}

	public static int zoom(int tile)
	{
		return (tile >>> 24) & 0xf;		// mask off nibble to allow top nibble to be used for flags
	}

	public static int tilesAtZoom(int zoom)
	{
		return (1 << zoom) << zoom;
	}

	/**
	 * Returns the width/height of a tile at the given zoom level
	 *
	 * @param zoom	a valid zoom level
	 * @return	width/height in pixels
	 */
	public static long sizeAtZoom(int zoom)
	{
		return 1L << (32-zoom);
		// This must be a long since int overflows for zoom 0 and zoom 1
	}

	public static int zoomFromSize(long size)
	{
		return Long.numberOfLeadingZeros(size) - 31;
	}
	
	/**
	 * Creates a tile number. This method does not check whether 
	 * the given column, row, and zoom level are valid.
	 * 
	 * @param col		a valid column
	 * @param row		a valid row
	 * @param zoom		a valid zoom level
	 * @return
	 */
	public static int fromColumnRowZoom(int col, int row, int zoom)
	{
		return (zoom << 24) | (row << 12) | col;
	}
	
	/**
	 * Determines the tile to which a coordinate belongs;
	 * coordinates must be in the projection used by the
	 * Mercator class.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int fromXYZ(int x, int y, int zoom)
	{
		// int col = (x >> (32-zoom)) + (1 << (zoom-1));
		// int row = -(y >> (32-zoom)) + (1 << (zoom-1)) -1;
		int col = columnFromXZ(x, zoom);
		int row = rowFromYZ(y, zoom);
		return fromColumnRowZoom(col, row, zoom);
	}

	public static int columnFromXZ(int x, int zoom)
	{
		// return (x >> (32-zoom)) + (1 << (zoom-1));
		return (int)(((long)x + (1L << 31)) >> (32-zoom));
	}
	
	public static int rowFromYZ(int y, int zoom)
	{
		return (int)(((long)Integer.MAX_VALUE - y) >> (32 - zoom));
		// return -(y >> (32-zoom)) + (1 << (zoom-1)) -1;
	}
	
	public static int fromXYZ(double x, double y, int zoom)
	{
		return fromXYZ((int)Math.round(x), (int)Math.round(y), zoom);
	}

	
	/**
	 * Checks if the given number represents a valid tile number
	 * 
	 * @param tile
	 * @return
	 */
	public static boolean isValid(int tile)
	{
		int zoom = zoom(tile);
		if (zoom > 12) return false;
		int maxRowCols = 1 << zoom;
		return column(tile) < maxRowCols && row(tile) < maxRowCols;
	}
	
	/**
	 * Returns the leftmost (lowest) x-coordinate that lies within
	 * the given tile
	 * 
	 * @param tile
	 * @return
	 */
	public static int leftX(int tile)
	{
		int zoom = zoom(tile);
		int col = column(tile);
		return (col-(1 << (zoom-1))) << (32-zoom);
	}

	/**
	 * Returns the rightmost (highest) x-coordinate that lies within
	 * the given tile
	 *
	 * @param tile
	 * @return
	 */
	public static int rightX(int tile)
	{
		int left = leftX(tile);
		int zoom = zoom(tile);
		long extent = 1L << (32 - zoom);
		return (int) (left + extent - 1);
	}

	/**
	 * Returns the bottom (lowest) y-coordinate that lies within 
	 * the given tile. Remember, going from top to bottom, tile
	 * rows *increase*, while y-coordinates *decrease*. 
	 * 
	 * @param tile
	 * @return
	 */
	public static int bottomY(int tile)
	{
		/*
		int zoom = zoom(tile);
		int row = row(tile);
		return (-(row-(1 << (zoom-1))) -1) << (32-zoom);
		 */
		return Integer.MIN_VALUE - (int)((long)(row(tile)+1) << (32 - zoom(tile)));
			// << 32 wraps around for int, that's why we cast to long
	}

	public static int topY(int tile)
	{
		return Integer.MAX_VALUE - (row(tile) << (32 - zoom(tile)));
		// return bottomY(tile) + (1 << (32-zoom(tile))) - 1;
	}

	/**
	 * Returns the tile that contains this tile at the specified 
	 * (lower) zoom level. If the zoom level is the same, the 
	 * tile itself is returned. 
	 * 
	 * @param tile	the tile	
	 * @param zoom	zoom level of the parent tile 
	 *              (must be <= the tile's zoom level)
	 * @return	the lower-zoom tile that contains the tile
	 */
	public static int zoomedOut(int tile, int zoom)
	{
		int currentZoom = zoom(tile);
		assert currentZoom >= zoom: String.format("Can't zoom out from %d to %d", currentZoom, zoom);
		int delta = currentZoom-zoom;
		return fromColumnRowZoom(column(tile) >> delta, row(tile) >> delta, zoom);
	}

	/**
	 * Returns the tile number of an adjacent tile that lies
	 * in the specified direction.
	 * 
	 * @param fromTile  the current tile
	 * @param direction the direction in which the neighbor
	 * tile is located
	 * @return the tile number of the adjacent tile
	 */
	public static int neighbor(int fromTile, Heading direction)
	{
		int zoom = zoom(fromTile);
		int col = column(fromTile);
		int row = row(fromTile);
		int mask = (1 << zoom) - 1;
		col = (col + direction.eastFactor()) & mask;
		row = (row - direction.northFactor()) & mask;
			// Heading assumes planar coordinates (north increases),
			// while tiles use screen coordinates (north decreases)
		return fromColumnRowZoom(col, row, zoom);
	}
	
	public static Box bounds(int tile)
	{
		int zoom = zoom(tile);
		int left = leftX(tile);
		int bottom = bottomY(tile);
		long extent = 1L << (32-zoom);
	
		return new Box(left, bottom, (int)(left+extent-1), (int)(bottom+extent-1));
	}

	public static Polygon polygon(int tile)
	{
		int zoom = zoom(tile);
		int left = leftX(tile);
		int bottom = bottomY(tile);
		long extent = 1L << (32-zoom);
		return GeometryBuilder.instance.createPolygon(
			new BoxCoordinateSequence(left, bottom,
				(int)(left+extent-1), (int)(bottom+extent-1)));
	}

	/**
	 * Returns the top-left tile occupied by a bounding box.
	 * 
	 * @param bbox	the bounds
	 * @param zoom	the zoom level for which to return the tile
	 * @return
	 */
	public static int topLeft(Bounds bbox, int zoom)
	{
		return fromXYZ(bbox.minX(), bbox.maxY(), zoom);
	}
	
	/**
	 * Returns the bottom-right tile occupied by a bounding box.
	 * 
	 * @param bbox	the bounds
	 * @param zoom	the zoom level for which to return the tile
	 * @return
	 */
	public static int bottomRight(Bounds bbox, int zoom)
	{
		return fromXYZ(bbox.maxX(), bbox.minY(), zoom);
	}
	
	public static String toString(int tile)
	{
		return String.format("%d/%d/%d", zoom(tile), column(tile), row(tile));
	}

	/**
	 * Parses a tile number from a String. Valid formats are:
	 * 		"zoom/column/row" or "column/row" (assumes zoom 12)
	 * 
	 * @param s
	 * @return the tile number, or -1 if the String does not represent a valid tile number
	 */
	public static int fromString(String s)
	{
		int zoom;
		String colString, rowString;
		int n = s.indexOf('/'); 
		if(n < 0) return -1;
		int n2 = s.indexOf('/', n+1);
		try
		{
			if(n2 < 0)
			{
				zoom = 12;
				colString = s.substring(0, n);
				rowString = s.substring(n+1);
			}
			else
			{
				zoom = Integer.parseInt(s.substring(0, n));
				if(zoom < 0 || zoom > 12) return -1;
				colString = s.substring(n+1, n2);
				rowString = s.substring(n2+1);
			}
			int col = Integer.parseInt(colString);
			int row = Integer.parseInt(rowString);
			int extent = (1 << zoom);
			if(col < 0 || col >= extent || row < 0 || row >= extent) return -1;
			return fromColumnRowZoom(col, row, zoom);
		}
		catch(NumberFormatException ex)
		{
			return -1;
		}
	}

	/**
	 * Checks whether the tile would be black, if we imagine the tile grid being laid
	 * out like a checkerboard (with the top left tile being white).
	 * 
	 * This allows us to implement an optimization strategy that significantly reduces
	 * the number of tiles we need to load to obtain the member ways of a large relation:
	 * We can check the color of the tiles where a way's start and end nodes are located,
	 * and preferably assign a uniform color tile as the home tile.
	 * 
	 * @param tile
	 * @return
	 */
	public static boolean isBlack(int tile)
	{
		return ((tile ^ (tile >> 12)) & 1) != 0;
	}
	
	public static Envelope intersection(int tile, Envelope env)
	{
		int extent = 1 << (32-zoom(tile));
		int tileMinX = leftX(tile);
		int tileMinY = bottomY(tile);
		int tileMaxX = tileMinX + extent - 1;
		int tileMaxY = tileMinY + extent - 1;
		return new Envelope(
			tileMinX > env.getMinX() ? tileMinX : env.getMinX(),
			tileMaxX < env.getMaxX() ? tileMaxX : env.getMaxX(),
			tileMinY > env.getMinY() ? tileMinY : env.getMinY(),
			tileMaxY < env.getMaxY() ? tileMaxY : env.getMaxY());
	}

	/**
	 * Calculates a new BoundingBox that represents the area of overlap
	 * between the given bounds and the bounding box of a tile
	 *  
	 * @param tile		the tile to which to constrain the bounds
	 * @param bounds
	 * @return
	 */
	public static Box intersection(int tile, Bounds bounds)
	{
		int extent = 1 << (32-zoom(tile));
		int tileMinX = leftX(tile);
		int tileMinY = bottomY(tile);
		int tileMaxX = tileMinX + extent - 1;
		int tileMaxY = tileMinY + extent - 1;
		return new Box(
			tileMinX > bounds.minX() ? tileMinX : bounds.minX(),
			tileMinY > bounds.minY() ? tileMinY : bounds.minY(),
			tileMaxX < bounds.maxX() ? tileMaxX : bounds.maxX(),
			tileMaxY < bounds.maxY() ? tileMaxY : bounds.maxY());
	}

	/**
	 * Returns the tile occupied by the given bounding box, or -1 if its extends
	 * across multiple tiles.
	 * 
	 * @param bbox	the bounding box
	 * @param zoom	the zoom level of the tile
	 * @return
	 */
	public static int fromBounds(Bounds bbox, int zoom)
	{
		int topLeft = topLeft(bbox, zoom);
		int bottomRight = bottomRight(bbox, zoom);
		return topLeft==bottomRight ? topLeft : -1;
	}

	// not used
	public static TileBox childrenOfTileAtZoom(int tile, int zoom)
	{
		int levels = zoom - Tile.zoom(tile);
		assert levels >= 0;
		int top = Tile.row(tile) << levels;
		int left = Tile.column(tile) << levels;
		int size = 1 << levels;
		TileBox box = new TileBox();
		box.expandToInclude(Tile.fromColumnRowZoom(left, top, zoom));
		box.expandToInclude(Tile.fromColumnRowZoom(left+size-1, top+size-1, zoom));
		return box;
	}

    // TODO: only works for positive deltas, and does not wrap!
	public static int relative(int tile, int deltaCol, int deltaRow)
	{
		return tile + (deltaRow << 12) + deltaCol;
	}

}
