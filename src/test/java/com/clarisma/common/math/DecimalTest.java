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
