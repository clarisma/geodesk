/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.core;

import java.util.function.IntConsumer;

// used only by tile.childrenOfTileAtZoom
public class TileBox 
{
	protected int topLeft;
	protected int bottomRight;

	public TileBox()
	{
		topLeft = -1;
		bottomRight = -1;
	}
	
	public final int width()
	{
		return Tile.column(bottomRight) - Tile.column(topLeft) + 1;
	}

	public final int height()
	{
		return Tile.row(bottomRight) - Tile.row(topLeft) + 1;
	}
	
	public int left()
	{
		return Tile.column(topLeft);
	}
	
	public int top()
	{
		return Tile.row(topLeft);
	}
	
	public int right()
	{
		return Tile.column(bottomRight);
	}
	
	public int bottom()
	{
		return Tile.row(bottomRight);
	}
	
	public int size()
	{
		return width() * height();
	}

	public int zoom()
	{
		return Tile.zoom(topLeft);
	}
	
	public void zoomOut(int newZoom)
	{
		int currentZoom = zoom();
		assert newZoom <= currentZoom;
		if(topLeft == -1 || newZoom==currentZoom) return;
		topLeft = Tile.zoomedOut(topLeft, newZoom);
		bottomRight = Tile.zoomedOut(bottomRight, newZoom);
	}

	public void expandToInclude(int tile)
	{
		if(topLeft == -1)
		{
			topLeft = tile;
			bottomRight = tile;
			return;
		}
		int zoom = Tile.zoom(tile);
		int currentZoom = zoom();
		if(zoom != currentZoom) 
		{
			if(zoom < currentZoom)
			{
				zoomOut(zoom);
			}
			else
			{
				tile = Tile.zoomedOut(tile, currentZoom);
			}
		}
		int col = Tile.column(tile);
		int row = Tile.row(tile);
		int left = Tile.column(topLeft);
		int top = Tile.row(topLeft);
		int right = Tile.column(bottomRight);
		int bottom = Tile.row(bottomRight);
		topLeft = Tile.fromColumnRowZoom(Integer.min(col, left), Integer.min(row, top), zoom);
		bottomRight = Tile.fromColumnRowZoom(Integer.max(col, right), Integer.max(row, bottom), zoom);
	}
	
	public void clear()
	{
		topLeft = -1;
		bottomRight = -1;
	}
	
	public void forEach(IntConsumer func)
	{
		int zoom = Tile.zoom(topLeft);
		int top = top();
		int left = left();
		int right = right();
		int bottom = bottom();
		
		for(int row = top; row<=bottom; row++)
		{
			for(int col = left; col<=right; col++)
			{
				func.accept(Tile.fromColumnRowZoom(col, row, zoom));
			}
		}
	}
}
