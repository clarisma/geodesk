/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.clarisma.common.math.Decimal;

public class TagValues
{
    /**
     * The last entry in the Global String Table that can serve as a
     * Global-Key code. Any strings with a code above this number must be
     * stored as a Local String if they are used as a tag key.
     */
    public static final int MAX_COMMON_KEY = (1 << 13) - 2;
    // We don't allow 0x1fff, because we use 0xffff as
        // "empty tagtable" marker;
        // TODO: use 0x8000 as
        // marker instead?
        // TODO: revisit empty marker; empty table is 4 bytes, but has
        //  wide-bit set; this complicates TileReader implementation
        //  Safest: 0x0000_8000
    public static final int EMPTY_TABLE_MARKER = 0xffff_ffff;
    public static final int MIN_NUMBER = -256;
    public static final int MAX_WIDE_NUMBER = (1 << 30) - 1 + MIN_NUMBER;
    public static final int MAX_NARROW_NUMBER = (1 << 16) - 1 + MIN_NUMBER;

    public static final int NARROW_NUMBER = 0;
    public static final int GLOBAL_STRING = 1;
    public static final int WIDE_NUMBER = 2;
    public static final int LOCAL_STRING = 3;

    /**
     * Checks whether the given decimal can be represented as a narrow number.
     *
     * @param decimal a long value in the format used by Decimal
     * @return
     */
    public static boolean isNarrowNumber(long decimal)
    {
        if(Decimal.scale(decimal) != 0) return false;
        long mantissa = Decimal.mantissa(decimal);
        return mantissa >= MIN_NUMBER && mantissa <= MAX_NARROW_NUMBER;
    }

    public static double wideNumberToDouble(int number)
    {
        double mantissa = (number >>> 2) + TagValues.MIN_NUMBER;
        switch(number & 3)
        {
        case 1: return mantissa / 10;
        case 2: return mantissa / 100;
        case 3: return mantissa / 1000;
        default: return mantissa;
        }
    }

    public static String wideNumberToString(int number)
    {
        int mantissa = (number >>> 2) + TagValues.MIN_NUMBER;
        switch(number & 3)
        {
        case 0: return String.valueOf(mantissa);
        case 1: return String.format("%.1f", ((double)mantissa / 10));
        case 2: return String.format("%.2f", ((double)mantissa / 100));
        case 3: return String.format("%.3f", ((double)mantissa / 1000));
        }
        return null; // cannot reach this
    }

    public static int toInt(String s)
    {
        try
        {
            return Integer.parseInt(s);
                // TODO: use more lenient parser that stops at non-digits
                //  instead of rejecting string, so we can parse "50mph" as 50
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
    }
}
