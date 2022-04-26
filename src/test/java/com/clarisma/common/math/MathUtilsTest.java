package com.clarisma.common.math;

import org.junit.Test;

import static org.junit.Assert.*;
import static com.clarisma.common.math.MathUtils.*;

public class MathUtilsTest
{
    @Test
    public void testDoubleFromString()
    {
        assertEquals(457, doubleFromString("457"), 0);
        assertEquals(457, doubleFromString("457.0"), 0);
        assertEquals(457, doubleFromString("457.000000000000000"), 0);
        assertEquals(0, doubleFromString("-00000.000000000000000"), 0);
        assertEquals(-13100, doubleFromString("-0013100.0000000000000000"), 0);
        assertEquals(-13100.999, doubleFromString("-0013100.999000000000000000"), 0);
        assertEquals(-1413100.99, doubleFromString("   -001413100.99abc9000000000000000"), 0);
    }
}