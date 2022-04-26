package com.geodesk.feature.old;

import com.geodesk.feature.Feature;
import com.geodesk.geom.Bounds;

// not used
public interface Query extends Bounds, Iterable<Feature>
{
	public static final int POINTS = 1;
	public static final int LINES = 2;
	public static final int AREAS = 4;
	public static final int RELATIONS = 8;
	
	int featureTypes();
	int count();
	Query in(int minX, int minY, int maxX, int maxY);
	Query in(Bounds bbox);
}
