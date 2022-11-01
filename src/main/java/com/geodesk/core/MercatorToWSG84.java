/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
