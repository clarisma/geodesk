/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.feature.FeatureType;

import java.util.Arrays;

// TODO: rename to TypedId?

/**
 * Methods for creating IDs that are unique across feature types.
 */
public class FeatureId
{
	public static long of(FeatureType type, long id)
	{
		return (id << 2) | type.ordinal(); 
	}
	
	public static long of(int type, long id)
	{
		assert type >= 0 && type <= 2; 
		return (id << 2) | type; 
	}

	public static long ofNode(long id)
	{
		return id << 2; 
	}

	public static long ofWay(long id)
	{
		return (id << 2) | 1; 
	}

	public static long ofRelation(long id)
	{
		return (id << 2) | 2; 
	}

	public static long id(long id)
	{
		return id >>> 2;
	}
	
	public static FeatureType type(long id)
	{
		return FeatureType.values()[(int)id & 3];
	}
	
	public static int typeCode(long id)
	{
		return (int)id & 3;
	}
	
	public static long fromString(String s)
	{
		int n = s.indexOf('/');
		if(n < 0) return 0;
		FeatureType type = FeatureType.from(s.substring(0,n));
		if(type==null) return 0;
		return of(type, Long.parseLong(s.substring(n+1)));
	}

	public static int compare(long fid1, long fid2)
	{
		int t1 = (int)fid1 & 3;
		int t2 = (int)fid2 & 3;
		if(t1 < t2) return -1;
		if(t1 > t2) return 1;
		return Long.compare(fid1, fid2);
	}
	
	public static void sort(long[] fids)
	{
		for(int i=0; i<fids.length; i++)
		{
			long fid = fids[i];
			fids[i] = (fid >> 2) | ((fid & 3) << 61);
		}
		Arrays.sort(fids);
		for(int i=0; i<fids.length; i++)
		{
			long fid = fids[i];
			fids[i] = ((fid << 2) & 0x7fff_ffff_ffff_ffffL) | (fid >>> 61);
		}
	}
	
	public static String typeToString(long fid)
	{
		switch((int)fid & 3)
		{
		case 0: return "node";
		case 1: return "way";
		case 2: return "relation";
		}
		return "";
	}

	public static boolean isNode(long fid)
	{
		return ((int)fid & 3) == 0;
	}

	public static boolean isWay(long fid)
	{
		return ((int)fid & 3) == 1;
	}

	public static boolean isRelation(long fid)
	{
		return ((int)fid & 3) == 2;
	}
	
	public static String toString(long fid)
	{
		return String.format("%s/%d", typeToString(fid), fid >>> 2);
	}
}
