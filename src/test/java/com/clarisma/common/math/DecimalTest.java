package com.clarisma.common.math;

import static org.junit.Assert.*;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class DecimalTest extends TestCase
{
	private static void test(String s)
	{
		long d = Decimal.parse(s, true);
		System.out.format("%s strict  -> %s\n", s, Decimal.toString(d));
		d = Decimal.parse(s, false);
		System.out.format("%s lenient -> %s\n", s, Decimal.toString(d));
		System.out.format("%s long    -> %d\n", s, Decimal.toLong(d));
		System.out.format("%s double  -> %f\n", s, Decimal.toDouble(d));
		System.out.format("%s normal  -> %s\n", s, Decimal.toString(Decimal.normalized(d)));
	}

    private static void test(String original,
        boolean strict, double expected, String expectedStr)
    {
        long d = Decimal.parse(original, strict);
        Assert.assertEquals(expected, Decimal.toDouble(d), 0.0000001);
        Assert.assertEquals(expectedStr, Decimal.toString(d));
    }

    @Test public void testDecimal()
    {
        test(".5", false, 0.5, "0.5");
        test(".5", true, Double.NaN, "invalid");

        test("", false, Double.NaN, "invalid");
        test("", true, Double.NaN, "invalid");

        test("0", false, 0.0, "0");
        test("0", true, 0.0, "0");

        test("007", false, 7, "7");
        test("007", true, Double.NaN, "invalid");

        test("08135", false, 8135, "8135");
        test("08135", true, Double.NaN, "invalid");

        test("3.5 t", false, 3.5, "3.5");
        test("3.5 t", true, Double.NaN, "invalid");

        test("50", false, 50.0, "50");
        test("50", true, 50.0, "50");

        test("01", false, 1.0, "1");
        test("01", true, Double.NaN, "invalid");

        test("0.0", true, Double.NaN, "invalid");
        test("0.00", true, Double.NaN, "invalid");

        test("0.500", false, 0.5, "0.500");
        test("0.500", true, Double.NaN, "invalid");

        test("00.500", false, 0.5, "0.500");
        test("00.500", true, Double.NaN, "invalid");

        test("0.", false, 0.0, "0");
        test("0.", true, Double.NaN, "invalid");

        test(".25", false, 0.25, "0.25");
        test(".25", true, Double.NaN, "invalid");

        test("-0.0000", false, 0.0, "0.0000");
        test("-0.0000", true, Double.NaN, "invalid");

        test("4.25.", false, 4.25, "4.25");
        test("4.25.", true, Double.NaN, "invalid");

        test("1000000000000000000000000000", false, Double.NaN, "invalid");
        test("1000000000000000000000000000", true, Double.NaN, "invalid");
    }

	

    /*
	@Test public void testParse()
	{
		test("1234.567");
		test("1234.0000");
		test("1.120");
		test("-900");
		test("-12.000");
		test("0");
		test("0.5");
		test("0.500");
		test("000.35");
		test("12.");
		test(".6");
		test(".700");
		test(".");
		test("-.3");
		test("-.");
		test("000.35");
		test("0.5");
		test("-0");
		test("-0.1234567890123");
		test("0.123456789012345678");
	}
	*/

	@Test public void testToString()
	{
		Assert.assertEquals("0.01", Decimal.toString(Decimal.of(1,2)));
		Assert.assertEquals("-0.003", Decimal.toString(Decimal.of(-3,3)));
		Assert.assertEquals("0.0000", Decimal.toString(Decimal.of(0,4)));
		Assert.assertEquals("33.000", Decimal.toString(Decimal.of(33000, 3)));
		Assert.assertEquals("2.1", Decimal.toString(Decimal.of(21, 1)));
		Assert.assertEquals("-55.22", Decimal.toString(Decimal.of(-5522, 2)));
		Assert.assertEquals("-1042.5799000", Decimal.toString(Decimal.of(-10425799000L, 7)));
		Assert.assertEquals("107", Decimal.toString(Decimal.of(107,0)));
		Assert.assertEquals("-4455", Decimal.toString(Decimal.of(-4455,0)));
		Assert.assertEquals("0", Decimal.toString(Decimal.of(0,0)));
		Assert.assertEquals("345678901234567890", Decimal.toString(Decimal.of(345678901234567890L,0)));
		Assert.assertEquals("-345678901234567890", Decimal.toString(Decimal.of(-345678901234567890L,0)));
	}
}
