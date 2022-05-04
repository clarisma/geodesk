package com.clarisma.common.math;

import com.clarisma.common.util.Log;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.clarisma.common.math.MathUtils.*;

public class MathUtilsTest
{
    @Test
    public void testDoubleFromString()
    {
        assertTrue("Must be Nan", Double.isNaN(doubleFromString("Test")));
        assertTrue("Must be Nan", Double.isNaN(doubleFromString("--2")));
        assertTrue("Must be Nan", Double.isNaN(doubleFromString("..5")));
        assertTrue("Must be Nan", Double.isNaN(doubleFromString("-..5")));
        assertEquals(457, doubleFromString("457"), 0);
        assertEquals(457, doubleFromString("457.0"), 0);
        assertEquals(457, doubleFromString("457.000000000000000"), 0);
        assertEquals(0, doubleFromString("-00000.000000000000000"), 0);
        assertEquals(-13100, doubleFromString("-0013100.0000000000000000"), 0);
        assertEquals(-13100.999, doubleFromString("-0013100.999000000000000000"), 0);
        assertEquals(-1413100.99, doubleFromString("   -001413100.99abc9000000000000000"), 0);

        /*
        // Log.debug("%.50f", doubleFromString("99.9999999999999999999"));
        Log.debug("%.50f", 99.9999999999999999999d);
        Log.debug("%.50f", Double.valueOf("99.9999999999999999999"));
        Log.debug("%.50f", Math.pow(.1, 17) * 9999999999999999999d);
        assertEquals(
            Double.valueOf("99.9999999999999999999"),
            doubleFromString("99.9999999999999999999"), 0);
         */
    }
}