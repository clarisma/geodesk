package com.geodesk.core;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.junit.Test;

public class TileSlotTest 
{
	private static final Logger log = LogManager.getLogger();

	@Test
	public void test() 
	{
		MutableIntSet slots = new IntHashSet();
		for(int z=4; z<=12; z+=4)
		{
			int max = 1 << z;
			for(int y=0; y<max; y++)
			{
				for(int x=0; x<max; x++)
				{
					int tile = Tile.fromColumnRowZoom(x, y, z);
					int slot = TileSlot.slotFromTile(tile);
					// log.debug("Zoom {}: X = {}, Y = {}", z, x, y);
					// log.debug("  Tile {} = Slot {}", tile, slot);
					assertTrue(TileSlot.tileFromSlot(slot) == tile);
					assertFalse(slots.contains(slot));
					slots.add(slot);
				}
			}
		}
	}

}
