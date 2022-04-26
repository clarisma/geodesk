package com.geodesk.core;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import static com.geodesk.core.TileQuad.*;

public class TileQuadTest 
{
	private static final Logger log = LogManager.getLogger();

	@Test
	public void test() 
	{
		testNormalize("10/534/348", SW | SE, "10/534/349", NW | NE);
		testSubtractQuads(
			"10/534/348", NW | NE | SW | SE, 
			"10/534/348", NW | NE, 
			"10/534/349", NW | NE);
		testQuadCoversQuad("10/538/325", NW, "10/538/324", NW, false);
		testQuadCoversQuad("10/539/330", NW | NE, "12/2160/1322", NW, true);
		testAddQuads("10/540/330", NW, "12/2160/1322", NW, "10/540/330", NW);
		testAddQuads("12/2160/1322", NW, "10/540/330", NW, "10/540/330", NW);
		testAddQuads("12/2160/1322", NW, "12/2163/1322", NW, "11/1080/661", NW | NE);
		testSubtractQuads("12/2160/1322", NW, "12/2163/1322", NW, "12/2160/1322", NW);

		// Relation in 12/2160/1322▕█▏, member in 12/2160/1323 ▄▏
		
		int t1 = Tile.fromString("12/2115/1363");
		int t2 = Tile.fromString("10/529/341");
		int t3 = Tile.fromString("12/2120/1364");
		int t4 = Tile.fromString("12/2120/1365");
		int tWorldTopLeft = Tile.fromString("12/0/0");
		int tWorldBottomRight = Tile.fromString("12/4095/4095");
		int tSouthEastOfNullIsland = Tile.fromString("12/2048/2048");
			
		int q1 = TileQuad.fromSingleTile(t1);
		int q2 = TileQuad.fromSingleTile(t2);
		int q3 = TileQuad.fromSingleTile(t3);
		int q4 = TileQuad.fromSingleTile(t4);
		int q5 = t3 | NW | SW;
		int q6 = t3 | NE | SE;
		
		assertEquals(Tile.fromString("10/528/340") | NW | SE, addQuad(q1,q2));
		assertEquals(Tile.fromString("9/264/170") | NW | NE, addQuad(q1,q3));
		assertEquals(Tile.fromString("10/529/341") | NW | NE, addQuad(q2,q3));
		assertEquals(Tile.fromString("12/2120/1364") | NW | SW, addQuad(q3,q4));
		
		assertEquals(Tile.fromString("10/411/363") | NW | NE | SE, 
			addQuad(Tile.fromString("10/411/363") | NW | NE,
					Tile.fromString("10/412/364") | NW));
		
		assertEquals(Tile.fromString("8/190/221") | NW | SW, 
			addQuad(Tile.fromString("8/190/222") | NW,
					Tile.fromString("8/190/221") | NW));
		
		assertEquals(Tile.fromString("8/190/221") | NW | NE, 
			subtractQuad(Tile.fromString("8/190/221") | NW | NE | SE ,
					Tile.fromString("8/191/222") | NW));
			
		
		log.debug("Original:     {}",  TileQuad.toString(q1));
		log.debug("Zoomed to 11: {}",  TileQuad.toString(TileQuad.zoomedOut(q1,11)));
		log.debug("Zoomed to 10: {}",  TileQuad.toString(TileQuad.zoomedOut(q1,10)));
		log.debug("q1      = {}", TileQuad.toString(q1));
		log.debug("q2      = {}", TileQuad.toString(q2));
		log.debug("q3      = {}", TileQuad.toString(q3));
		log.debug("q4      = {}", TileQuad.toString(q4));
		log.debug("q5      = {}", TileQuad.toString(q5));
		log.debug("q6      = {}", TileQuad.toString(q6));
		log.debug("q1 + q2 = {}", TileQuad.toString(TileQuad.addQuad(q1,q2)));
		log.debug("q1 + q3 = {}", TileQuad.toString(TileQuad.addQuad(q1,q3)));
		log.debug("q2 + q3 = {}", TileQuad.toString(TileQuad.addQuad(q2,q3)));
		log.debug("q3 + q4 = {}", TileQuad.toString(TileQuad.addQuad(q3,q4)));
		log.debug("{}", TileQuad.toString(TileQuad.of(tWorldTopLeft, tWorldBottomRight)));
		int q7 = TileQuad.of(tSouthEastOfNullIsland, tWorldBottomRight);
		log.debug("{}", TileQuad.toString(q7));
		log.debug("{}  zoomed out to 1 = {}", TileQuad.toString(q7), 
			TileQuad.toString(TileQuad.zoomedOut(q7, 1)));
		log.debug("{}  zoomed out to 0 = {}", TileQuad.toString(q7),
			TileQuad.toString(TileQuad.zoomedOut(q7, 0)));
		
		int q8 = TileQuad.fromSingleTile("2/3/3");
		log.debug("{}  zoomed out to 1 = {}", TileQuad.toString(q8), 
			TileQuad.toString(zoomedOut(q8, 1)));
		log.debug("{}  zoomed out to 0 = {}", TileQuad.toString(q8), 
				TileQuad.toString(zoomedOut(q8, 0)));
		
		int q9 = Tile.fromString("12/2047/2047") | NE | SW | SE;
		testQuadZooms(q9);
		q9 = Tile.fromString("12/3071/2047") | NE | SW | SE;
		testQuadZooms(q9);
		
		testTileZooms(tWorldTopLeft);
		testTileZooms(tWorldBottomRight);
		
		int q10 = Tile.fromString("12/3097/1092") | NW | SE;
		testQuadZooms(q10);
		
		testTileAddition(Tile.fromString("12/423/1092"), Tile.fromString("12/424/1092"));
		testTileAddition(Tile.fromString("12/423/1092"), Tile.fromString("12/425/1092"));
		testTileAddition(Tile.fromString("12/423/1092"), Tile.fromString("12/422/1092"));
		testTileAddition(Tile.fromString("12/423/1092"), Tile.fromString("12/422/1091"));
		testTileAddition(Tile.fromString("12/423/1092"), Tile.fromString("12/421/1090"));
		
		testNormalize("12/459/378", SE, "12/460/379", NW);
		testNormalize("6/4/5", NW, "6/4/5", NW);
		testNormalize("8/17/63", NE | SE, "8/18/63", NW | SW);
		testNormalize("8/17/63", NW | SW, "8/17/63", NW | SW);
		testNormalize("4/13/10", NW | NE | SW | SE, "4/13/10", NW | NE | SW | SE);
		testNormalize("4/13/10", SW | SE, "4/13/11", NW | NE);
		testNormalize("4/13/10", NW | SE, "4/13/10", NW | SE);
		testNormalize("4/13/10", NE | SW, "4/13/10", NE | SW);
		
		testBlackTiles();
	}
	
	private void testTileZooms(int t)
	{
		testQuadZooms(fromSingleTile(t));
	}
	
	private void testQuadZooms(int q)
	{
		int zoom = TileQuad.zoom(q);
		for(int i=zoom-1; i>=0; i--)
		{
			log.debug("{}  zoomed out to {} = {}", TileQuad.toString(q), i, 
				TileQuad.toString(zoomedOut(q, i)));
		}
	}

	private void testNormalize(String tile, int bits, String resTile, int resBits)
	{
		assertEqualQuads(
			Tile.fromString(resTile) | resBits, 
			normalize(Tile.fromString(tile) | bits));
	}
	
	private static void assertEqualQuads(int expected, int actual)
	{
		assert expected == actual: 
			String.format("Expected %s, but got %s",
				TileQuad.toString(expected), TileQuad.toString(actual));
	}

	private void testAddQuads(String aTile, int aBits, String bTile, int bBits, 
		String resTile, int resBits)
	{
		assertEqualQuads(Tile.fromString(resTile) | resBits, 
			addQuad(
				Tile.fromString(aTile) | aBits,
				Tile.fromString(bTile) | bBits));
	}

	private void testSubtractQuads(String aTile, int aBits, String bTile, int bBits, 
		String resTile, int resBits)
	{
		assertEqualQuads(Tile.fromString(resTile) | resBits, 
			subtractQuad(
				Tile.fromString(aTile) | aBits,
				Tile.fromString(bTile) | bBits));
	}

	private void testQuadCoversQuad(String aTile, int aBits, String bTile, int bBits, 
		boolean result)
	{
		assertEquals(TileQuad.coversQuad(
			Tile.fromString(aTile) | aBits, 
			Tile.fromString(bTile) | bBits), result);
	}

	
	private void testTileAddition(int t1, int t2)
	{
		log.debug("{} + {} = {}", 
			Tile.toString(t1), Tile.toString(t2),
			TileQuad.toString(TileQuad.addTile(t1 | NW, t2)));
	}
	
	private void testBlackTiles()
	{
		int col = 1232;
		int row = 739;
		int zoom = 12;
		
		for(int x=0; x<4; x++)
		{
			for(int y=0; y<4; y++)
			{
				for(int tiles=1; tiles<16; tiles++)
				{
					int tile = Tile.fromColumnRowZoom(col+x, row+y, zoom);
					int quad = tile | (tiles << 28);
					tile = TileQuad.blackTile(quad);
					// log.debug("Black tile of {} is {}",  TileQuad.toString(quad), Tile.toString(tile));
					assertTrue(TileQuad.containsTile(quad, tile));
				}
			}
		}
	}

	/*
	@Test public void testBlackTile()
	{
		int quad = Tile.fromString("9/271/175") | TileQuad.NE | TileQuad.SW | TileQuad.SE;
		int tile = TileQuad.blackTile(quad);
		log.debug("Black tile: {}", Tile.toString(tile));
	}
	 */

	@Test public void testOversizedSiblingLocator()
	{
		int startTile = Tile.fromString("12/2147/1982");
		Assert.assertEquals(OVERSIZED, toSparseSiblingLocator(OVERSIZED, startTile));
	}
}
