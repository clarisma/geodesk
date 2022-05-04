package com.geodesk.feature.filter;

import com.clarisma.common.math.MathUtils;
import com.geodesk.feature.Filter;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public abstract class TagFilter implements Filter
{
    protected final String[] globalStrings;
    protected final int keyMask;
    protected final int keyMin;

    protected TagFilter(String[] globalStrings, int keyMask, int keyMin)
    {
        this.globalStrings = globalStrings;
        this.keyMask = keyMask;
        this.keyMin = keyMin;
    }

    // TODO
    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return accept(buf, pos);
    }

    /*
    @Override public boolean accept(ByteBuffer buf, int pos, int roleGroup)
    {
        return accept(buf, pos);
    }

    @Override public int acceptRole(int roleCode, String roleString)
    {
        return 1;
    }
     */

    @Override public boolean acceptIndex(int keys)
    {
        return (keys & keyMask) >= keyMin;
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        throw new UnsupportedOperationException(
            "This Filter is not intended for geometries");
        // return true;
    }

    protected static String doubleToString(double d)
    {
        if(d == (long)d) return Long.toString((long)d);
        return Double.toString(d);
    }

    protected static double stringToDouble(String s)
    {
        return MathUtils.doubleFromString(s);
        /*
        try
        {
            return Double.parseDouble(s);
        }
        catch(NumberFormatException ex)
        {
            return Double.NaN;
        }
         */
    }

    protected String globalString(int code)
    {
        try
        {
            return globalStrings[code];
        }
        catch(ArrayIndexOutOfBoundsException ex)
        {
            // TODO: this is a sign of an invalid FeatureStore
            throw new QueryException(String.format(
                "Invalid global string code: %d", code));
        }
    }

    /*
    protected static void debug(String msg, int value)
    {
        FeatureStoreBase.log.debug(msg, value);
    }
     */
}
