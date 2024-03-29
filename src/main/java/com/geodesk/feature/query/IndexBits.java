/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

// TODO: get rid of this class?
public class IndexBits
{
    /**
     * Turns a category into a value that can be matched against a key-index
     * bitset.
     *
     * @param category  the index category (1-based; range 1 to 30)
     * @return an `int` with a single bit set to 1
     */
    public static int fromCategory(int category)
    {
        return category==0 ? 0 : (1 << (category-1));
    }

    // Note: category starts with 1, but categories[] is 0-based
    public static String toString(int bits,
        String[] categories, String separator, String uncategorized)
    {
        if(bits == 0) return uncategorized;
        StringBuilder buf = new StringBuilder();
        int cat = -1;
        while(bits != 0)
        {
            int zeroes = Integer.numberOfTrailingZeros(bits);
            cat += zeroes+1;
            if(buf.length() > 0) buf.append(separator);
            buf.append(categories[cat]);
            bits >>>= zeroes+1;
        }
        return buf.toString();
    }

    public static int firstCategory(int bits)
    {
        return bits==0 ? 0 : (Integer.numberOfTrailingZeros(bits) + 1);
    }
}
