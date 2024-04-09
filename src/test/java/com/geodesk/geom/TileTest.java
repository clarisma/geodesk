package com.geodesk.geom;

import org.junit.Test;

import static org.junit.Assert.*;

public class TileTest
{
    @Test public void testFromString()
    {
        assertEquals(0, Tile.fromString("0/0/0"));
        assertEquals(0x3007_006, Tile.fromString("3/6/7"));
        assertEquals(-1, Tile.fromString("12/4367/0"));
        assertEquals(-1, Tile.fromString("3/97/-4"));
        assertEquals(-1, Tile.fromString("not a valid tile"));
    }

    @Test public void testFromXYZ()
    {
        assertEquals(Tile.fromString("12/0/0"), Tile.fromXYZ(
            Integer.MIN_VALUE, Integer.MAX_VALUE, 12));
        assertEquals(Tile.fromString("0/0/0"), Tile.fromXYZ(
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0));
        assertEquals(Tile.fromString("0/0/0"), Tile.fromXYZ(
            Integer.MAX_VALUE, Integer.MIN_VALUE, 0));
        assertEquals(Tile.fromString("0/0/0"), Tile.fromXYZ(0,0,0));
    }

    @Test public void testBounds()
    {
        assertEquals(-2147483648, Tile.leftX(Tile.fromString("0/0/0")));
        assertEquals(-1073741824, Tile.leftX(Tile.fromString("3/2/0")));
        assertEquals(-1073741824, Tile.leftX(Tile.fromString("3/2/1")));
        assertEquals(-1073741824, Tile.leftX(Tile.fromString("3/2/4")));
        assertEquals(-787480576, Tile.leftX(Tile.fromString("12/1297/1162")));
        assertEquals(1099956224, Tile.leftX(Tile.fromString("12/3097/4000")));
        assertEquals(-1342177280, Tile.leftX(Tile.fromString("4/3/15")));
        assertEquals(-2013265920, Tile.leftX(Tile.fromString("6/2/44")));

        assertEquals(2147483647, Tile.topY(Tile.fromString("0/0/0")));
        assertEquals(-1, Tile.topY(Tile.fromString("1/0/1")));
        assertEquals(2147483647, Tile.topY(Tile.fromString("3/2/0")));
        assertEquals(1610612735, Tile.topY(Tile.fromString("3/2/1")));
        assertEquals(-1, Tile.topY(Tile.fromString("3/2/4")));
        assertEquals(929038335,Tile.topY(Tile.fromString("12/1297/1162")));
        assertEquals(-2046820353, Tile.topY(Tile.fromString("12/3097/4000")));
        assertEquals(-1879048193,Tile.topY(Tile.fromString("4/3/15")));
        assertEquals(-805306369, Tile.topY(Tile.fromString("6/2/44")));

        assertEquals("12/1297/1162", Tile.toString(Tile.fromXYZ(
            -787480576, 929038335, 12)));
        assertEquals(Tile.fromString("4/3/15"), Tile.fromXYZ(
            -1342177280, -1879048193, 4));

        assertEquals(-2147483648, Tile.bottomY(Tile.fromString("0/0/0")));
        assertEquals(-2147483648, Tile.bottomY(Tile.fromString("1/0/1")));
        assertEquals(0, Tile.bottomY(Tile.fromString("1/0/0")));
        assertEquals(1610612736, Tile.bottomY(Tile.fromString("3/2/0")));
        assertEquals(1073741824, Tile.bottomY(Tile.fromString("3/2/1")));
        assertEquals(-536870912, Tile.bottomY(Tile.fromString("3/2/4")));
        assertEquals(927989760,Tile.bottomY(Tile.fromString("12/1297/1162")));
        assertEquals(-2047868928, Tile.bottomY(Tile.fromString("12/3097/4000")));
        assertEquals(-2147483648,Tile.bottomY(Tile.fromString("4/3/15")));
        assertEquals(-872415232, Tile.bottomY(Tile.fromString("6/2/44")));

        assertEquals(1297, Tile.columnFromXZ(-787480576, 12));
        assertEquals(4095, Tile.columnFromXZ(0x7fff_ffff, 12));
        assertEquals(0, Tile.columnFromXZ(0x8000_0000, 12));
        assertEquals(1162, Tile.rowFromYZ(927989760, 12));
        assertEquals(1162, Tile.rowFromYZ(929038335, 12));
        assertEquals(4095, Tile.rowFromYZ(0x8000_0000, 12));
        assertEquals(0, Tile.rowFromYZ(0x7fff_ffff, 12));
        assertEquals(3, Tile.columnFromXZ(-1342177280, 4));
        assertEquals(15, Tile.rowFromYZ(-2147483648, 4));
        assertEquals(15, Tile.rowFromYZ(-1879048193, 4));
        assertEquals(15, Tile.columnFromXZ(0x7fff_ffff, 4));
        assertEquals(15, Tile.rowFromYZ(0x8000_0000, 4));
        assertEquals(0, Tile.rowFromYZ(0x7fff_ffff, 4));


        assertEquals(0, Tile.columnFromXZ(0x8000_0000, 4));
        assertEquals(0, Tile.columnFromXZ(0, 0));
        assertEquals(0, Tile.columnFromXZ(Integer.MIN_VALUE, 0));
        assertEquals(0, Tile.columnFromXZ(Integer.MAX_VALUE, 0));
        assertEquals(0, Tile.rowFromYZ(0, 0));
        assertEquals(0, Tile.rowFromYZ(Integer.MIN_VALUE, 0));
        assertEquals(0, Tile.rowFromYZ(Integer.MAX_VALUE, 0));
        assertEquals(1, Tile.columnFromXZ(0, 1));
        assertEquals(0, Tile.columnFromXZ(Integer.MIN_VALUE, 1));
        assertEquals(1, Tile.columnFromXZ(Integer.MAX_VALUE, 1));
        assertEquals(0, Tile.rowFromYZ(0, 1));
        assertEquals(1, Tile.rowFromYZ(-1, 1));
        assertEquals(1, Tile.rowFromYZ(Integer.MIN_VALUE, 1));
        assertEquals(0, Tile.rowFromYZ(Integer.MAX_VALUE, 1));

    }

    @Test public void test()
    {
        assertEquals(0, Tile.row(Tile.fromString("0/0/0")));
        assertEquals(0, Tile.column(Tile.fromString("0/0/0")));
        assertEquals(0, Tile.zoom(Tile.fromString("0/0/0")));

        assertEquals(Integer.MIN_VALUE, Tile.leftX(Tile.fromString("12/0/0")));
        assertEquals(Integer.MAX_VALUE, Tile.topY(Tile.fromString("12/0/0")));
        assertEquals(Integer.MAX_VALUE, Tile.topY(Tile.fromString("12/3567/0")));
        assertEquals(2146435072, Tile.bottomY(Tile.fromString("12/4031/0")));
        assertEquals(Integer.MAX_VALUE, Tile.topY(0));
        assertEquals(Integer.MIN_VALUE, Tile.bottomY(0));
    }
}