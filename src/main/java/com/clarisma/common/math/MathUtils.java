package com.clarisma.common.math;

// TODO: these are really parsing methods rather than "math"

public class MathUtils
{
    private static final double[] POW10 =
    {
        1, 10, 100, 1000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000
    };

    public static double pow10(int exp)
    {
        return exp >= 0 && exp < POW10.length ? POW10[exp] : Math.pow(10, exp);
    }

    public static double doubleFromString(String s)
    {
        int len = s.length();
        int i = 0;
        for(; i<len; i++) if(s.charAt(i) > 32) break;
        if(i >= len) return Double.NaN;
        boolean negative = false;
        int decimalPos = -1;
        double value = 0;
        if(s.charAt(i) == '-')
        {
            negative = true;
            i++;
        }
        for(; i<len; i++)
        {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9')
            {
                value = value * 10 + (ch - '0');
                continue;
            }
            if (ch == '.')
            {
                if (decimalPos >= 0) break;
                decimalPos = i;
                continue;
            }
            break;
        }
        if(negative) value = -value;
        return decimalPos < 0 ? value : (value / MathUtils.pow10(i-decimalPos-1));
    }

    /**
     * Counts the number of characters in the given <code>String</code> that
     * represent a valid number.
     *
     * Valid characters are:
     * - leading whitespace
     * - a leading minus sign
     * - digits and a single decimal point
     *
     * @param   s   the <code>String</code> to examine
     * @return  the first position that contains a character that is not
     *          part of a number
     */
    public static int countNumberChars(String s)
    {
        int n = 0;
        int len = s.length();
        for(; n<len; n++) if(s.charAt(n) > 32) break;
        if(n >= len) return 0;
        if(s.charAt(n) == '-') n++;
        boolean seenDecimalPoint = false;
        for(; n<len; n++)
        {
            char ch = s.charAt(n);
            if (ch >= '0' && ch <= '9') continue;
            if (ch == '.')
            {
                if (seenDecimalPoint) break;
                seenDecimalPoint = true;
                continue;
            }
            break;
        }
        return n;
    }
}
