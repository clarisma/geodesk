package com.geodesk.geom;

public class Morton 
{
	public static int mortonFromXY(int x, int y)
	{
		assert (x & 0xffff_0000) == 0;
		assert (y & 0xffff_0000) == 0;
		
		int i0 = x;
		int i1 = y;
		
		i0 = (i0 | (i0 << 8)) & 0x00FF00FF;
		i0 = (i0 | (i0 << 4)) & 0x0F0F0F0F;
		i0 = (i0 | (i0 << 2)) & 0x33333333;
		i0 = (i0 | (i0 << 1)) & 0x55555555;
		
		i1 = (i1 | (i1 << 8)) & 0x00FF00FF;
		i1 = (i1 | (i1 << 4)) & 0x0F0F0F0F;
		i1 = (i1 | (i1 << 2)) & 0x33333333;
		i1 = (i1 | (i1 << 1)) & 0x55555555;
		
		return (i1 << 1) | i0; 
	}
}
