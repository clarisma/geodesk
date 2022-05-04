package com.geodesk.feature.filter;

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