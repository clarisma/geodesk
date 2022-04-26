package com.clarisma.common.math;

public class DoubleRange implements Comparable<DoubleRange>
{
	private double start;
	private double end;
	private boolean includeStart;
	private boolean includeEnd;
	
	public DoubleRange(double start, double end, boolean includeStart, boolean includeEnd)
	{
		this.start = start;
		this.end = end;
		this.includeStart = includeStart;
		this.includeEnd = includeEnd;
	}

	public double start()
	{
		return start;
	}

	public double end()
	{
		return end;
	}

	public boolean includeStart()
	{
		return includeStart;
	}

	public boolean includeEnd()
	{
		return includeEnd;
	}
	
	@Override public boolean equals(Object other)
	{
		if(!(other instanceof DoubleRange)) return false;
		DoubleRange o = (DoubleRange)other;
		return start==o.start && includeStart==o.includeStart &&
			end==o.end && includeEnd==o.includeEnd;
	}

	@Override public int compareTo(DoubleRange other)
	{
		int comp = Double.compare(start, other.start);
		if(comp != 0) return comp;
		if(includeStart && !other.includeStart) return -1;
		if(!includeStart && other.includeStart) return 1;
		comp = Double.compare(end, other.end);
		if(comp != 0) return comp;
		if(!includeEnd && other.includeEnd) return -1;
		if(includeEnd && !other.includeEnd) return 1;
		return 0;
	}
	
	@Override public String toString()
	{
		return String.format("%c%f,%f%c", 
			includeStart ? '[' : '(', start,
			end, includeEnd ? ']' : ')');
	}
	
	public static DoubleRange intersection(DoubleRange a, DoubleRange b)
	{
		if(b.start > a.end) return null;
		if(b.start==a.end && !b.includeStart && !a.includeEnd) return null;
		if(a.start > b.end) return null;
		if(a.start==b.end && !a.includeStart && !b.includeEnd) return null;
		double start;
		double end;
		boolean includeStart;
		boolean includeEnd;
		if(a.start > b.start)
		{
			start = a.start;
			includeStart = a.includeStart;
		}
		else if (b.start > a.start)
		{
			start = b.start;
			includeStart = b.includeStart;
		}
		else
		{
			start = a.start;
			includeStart = a.includeStart ? b.includeStart : false;
		}
		if(a.end < b.end)
		{
			end = a.end;
			includeEnd = a.includeEnd;
		}
		else if (b.end < a.end)
		{
			end = b.end;
			includeEnd = b.includeEnd;
		}
		else
		{
			end = a.end;
			includeEnd = a.includeEnd ? b.includeEnd : false;
		}
		return new DoubleRange(start, end, includeStart, includeEnd);
	}
}
