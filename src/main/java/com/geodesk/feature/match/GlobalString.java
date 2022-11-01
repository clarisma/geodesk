/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

public class GlobalString implements Comparable<GlobalString>
{
	private final String stringValue;
	private final int value;
	
	public GlobalString(String stringValue, int value)
	{
		this.stringValue = stringValue;
		this.value = value;
	}
	
	public int value()
	{
		return value;
	}

	public String stringValue()
	{
		return stringValue;
	}

	public String toString()
	{
		return String.format("\"%s\" (#%d)", stringValue, value);
	}

	@Override public int compareTo(GlobalString other)
	{
		return Integer.compare(value, other.value);
	}
}