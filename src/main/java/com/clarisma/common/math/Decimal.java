/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.math;

public class Decimal extends Number 
{
	public static final long INVALID = Long.MIN_VALUE;
	
	private long value;
	
	private Decimal(long value)
	{
		this.value = value;
	}
	
	public static Decimal fromString(String s)
	{
		return new Decimal(parse(s, false));
	}

    public static long parse(String s, boolean strict)
    {
        long value = 0;
        int scale = 0;
        boolean seenZero = false;
        boolean seenNonZero = false;
        boolean leadingZeroes = false;
        boolean trailingNonNumeric = false;
        boolean seenDot = false;
        boolean negative = false;

        int len = s.length();
        if (len == 0) return INVALID;

        int i = 0;
        char first = s.charAt(i);
        if (first == '-')
        {
            negative = true;
            i++;
            if (i == len) return INVALID;
        }
        else if (first == '+')
        {
            if (strict) return INVALID;
            i++;
            if (i == len) return INVALID;
        }

        while (i < len)
        {
            char ch = s.charAt(i++);
            if (ch == '0')
            {
                leadingZeroes |= seenZero && !seenNonZero;
                seenZero = true;
                value *= 10;
                if ((value & 0xf800_0000_0000_0000L) != 0) return INVALID;
                continue;
            }
            if (ch == '.')
            {
                seenDot = true;
                while (i < len)
                {
                    ch = s.charAt(i++);
                    if (ch < '0' || ch > '9')
                    {
                        trailingNonNumeric = true;
                        break;
                    }
                    value = value * 10 + (ch - '0');
                    if ((value & 0xf800_0000_0000_0000L) != 0) return INVALID;
                    scale++;
                }
                break;
            }
            if (ch < '0' || ch > '9')
            {
                trailingNonNumeric = true;
                break;
            }
            leadingZeroes |= seenZero && !seenNonZero;
            seenNonZero = true;
            value = value * 10 + (ch - '0');
            if ((value & 0xf800_0000_0000_0000L) != 0) return INVALID;
        }

        if (strict)
        {
            if (trailingNonNumeric) return INVALID;
            if (seenDot && (scale == 0 || (!seenZero && !seenNonZero)))
            {
                return INVALID;
            }
            if (leadingZeroes) return INVALID;
            if (value == 0 && negative) return INVALID;
        }

        if (scale > 15) return INVALID;

        long result = (negative ? -value : value) << 4;
        return result | scale;
    }


	public static int scale(long d)
	{
		return (int)d & 15;
	}
	
	public static long mantissa(long d)
	{
		return d >> 4;
	}

	// TODO: use LUT instead of loop
	public static long toLong(long d)
	{
		if(d == INVALID) return d;
		int scale = (int)d & 15;
		if(scale==0) return (d >> 4);
		long div = 10;
		for(;;)
		{
			scale--;
			if(scale==0) break;
			div *= 10;
		}
		return (d >> 4) / div;
	}

	// TODO: use LUT instead of loop
	public static double toDouble(long d)
	{
		if(d == INVALID) return Double.NaN;
		int scale = (int)d & 15;
		long mantissa = d >> 4;
		if(scale==0) return mantissa;
		long div = 10;
		for(;;)
		{
			scale--;
			if(scale==0) break;
			div *= 10;
		}
		return ((double)mantissa) / div;
	}
	
	public static float toFloat(long d)
	{
		return (float)toDouble(d);
	}
	
	public static int toInt(long d)
	{
		return (int)toLong(d);
	}
	
	public static long of(long mantissa, int scale)
	{
		assert scale >= 0 && scale <= 15;
		return (mantissa << 4) | scale;
	}
	
	public static String toString(long d)
	{
		if(d == INVALID) return "invalid";
		int scale = (int)d & 15;
		String s = Long.toString(d >> 4);
		if(scale==0) return s;
		int len = s.length();
		char[] chars;
		if(d < 0)
		{
			if(len <= scale+1)
			{
				int n = scale-len+4;
				chars = new char[scale+3];
				s.getChars(1, len, chars, n);
				for(int i=1; i<n; i++) chars[i] = '0';
			}
			else
			{
				chars = new char[len+1];
				s.getChars(1, len-scale, chars, 1);
				s.getChars(len-scale, len, chars, chars.length-scale);
			}
			chars[0] = '-';
		}
		else
		{
			if(len <= scale)
			{
				int n = scale-len+2;
				chars = new char[scale+2];
				s.getChars(0, len, chars, n);
				for(int i=0; i<n; i++) chars[i] = '0';
			}
			else
			{
				chars = new char[len+1];
				s.getChars(0, len-scale, chars, 0);
				s.getChars(len-scale, len, chars, chars.length-scale);
			}
		}
		chars[chars.length-scale-1] = '.';
		return new String(chars);
	}
	
	public static long normalized(long d)
	{
		if(d == INVALID) return INVALID;
		int scale = (int)d & 15;
		long v = d >> 4;
		while(scale > 0)
		{
			long x = v / 10;
			if(x * 10 != v) break;
			scale--;
			v = x;
		}
		return (v << 4) | scale;
	}

	@Override public int intValue()
	{
		return toInt(value);
	}

	@Override public long longValue()
	{
		return toLong(value);
	}

	@Override public float floatValue()
	{
		return toFloat(value);
	}

	@Override public double doubleValue()
	{
		return toDouble(value);
	}
	
	@Override public String toString()
	{
		return toString(value);
	}
}
