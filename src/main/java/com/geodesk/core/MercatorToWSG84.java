package com.geodesk.core;

public class MercatorToWSG84 implements Projection
{
	public double projectX(double x) 
	{
		return Mercator.lonFromX(x);
	}

	public double projectY(double y) 
	{
		return Mercator.latFromY(y);
	}

}
