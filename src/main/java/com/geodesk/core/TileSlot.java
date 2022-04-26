package com.geodesk.core;

/**
 * An encoding that ensures that tile references are clustered along with 
 * their neighbors in order to improve locality of reference.
 */
// No longer needed
public class TileSlot 
{
	private static final int[] Y_SHIFT = { 0, 4, 0 }; // zoom 4 does not user upper Y 
	private static final int[] SLOT_START = { 512, 1024, 65 * 1024 };
	// private static final int[] X_MASK = { 0, 0x1c00, 0x1fc00 };
	
	/**
	 * There are only 256 tiles at zoom level 4, but is is more convenient to
	 * always use 5 bits to encode the tiles' X-coordinates. Therefore, zoom-4
	 * tiles take up 512 slots (half a page for 4-byte values). We'll place
	 * the first tile at slot 512, which leaves the first half of the page
	 * free for other administrative data. 
	 */
	public static final int UNUSED_LEADING_SLOTS = 512;
	public static final int TOTAL_SLOTS = SLOT_START[2] + 4096 * 4096;
	

	/**
	 * Turns a tile number into a slot index.
	 * 
	 * @param tile
	 * @return
	 */
	public static final int slotFromTile(int tile)
	{
		int zoom = Tile.zoom(tile);
		assert zoom==12 || zoom==8 || zoom==4;
		int n = (zoom >> 2)-1;
		int lowerX = tile & 0x1f;
		int lowerY = (tile >> 7) & 0x3e0;
		int upperX = (tile << 5) & 0x1fc00;
		int upperY = (tile & 0xfe0000) >> Y_SHIFT[n];
		return SLOT_START[n] + (lowerX | lowerY | upperX | upperY); 
	}
	
	public static final int tileFromSlot(int slot)
	{
		int zoom;
		int x;
		int y;
		if(slot >= SLOT_START[2])
		{
			zoom = 12;
			slot -= SLOT_START[2];
			x = ((slot & 0x1fc00) >> 5) | (slot & 0x1f);
			y = (slot & 0xfe0000) | ((slot & 0x3e0) << 7);
		}
		else if(slot >= SLOT_START[1])
		{
			zoom = 8;
			slot -= SLOT_START[1];
			x = ((slot & 0x1c00) >> 5) | (slot & 0x1f);
			y = ((slot & 0xe000) << 4) | ((slot & 0x3e0) << 7);
		}
		else
		{
			assert slot >= SLOT_START[0];
			zoom = 4;
			slot -= SLOT_START[0];
			x = slot & 0x1f;
			y = (slot & 0x1e0) << 7;
		}
		return (zoom << 24) | x | y;
	}

}
