/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

/// An enum representing the three feature types: `NODE`, `WAY` and `RELATION`.
// TODO: add toClass()
public enum FeatureType
{
	NODE, WAY, RELATION;
	
	public static FeatureType from(String s)
	{
		switch(s)
		{
		case "node":		return NODE;
		case "way":			return WAY;
		case "relation":	return RELATION;
		default:			
			throw new RuntimeException(s + " is not a valid feature type"); 
		}
	}
	
	public static String toString(FeatureType type)
	{
		// TODO: use type.ordinal() lookup in array
		switch(type)
		{
		case NODE: return "node";
		case WAY: return "way";
		case RELATION: return "relation";
		}
		return "";
	}
}