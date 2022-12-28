package com.clarisma.common.parser;

import com.clarisma.common.util.Log;
import org.junit.Assert;
import org.junit.Test;

public class SimpleParserTest
{
    public static class SchemaMaker
    {
        long lower;
        long upper;

        public void add(char ch)
        {
            Assert.assertTrue(ch > ' ' && ch < 128);
            if(ch < 64)
            {
                lower |= 1L << ch;
            }
            else
            {
                upper |= 1L << ch;
            }
        }

        public void addRange(char chStart, char chEnd)
        {
            Assert.assertTrue(chEnd > chStart);
            for(char ch=chStart; ch<=chEnd; ch++) add(ch);
        }

        public void print()
        {
            Log.debug("Lower = %s", Long.toString(lower, 2));
            Log.debug("Upper = %s", Long.toString(upper, 2));
        }
    }

    @Test public void prepareSchema()
    {
        SchemaMaker schema = new SchemaMaker();
        schema.add('_');
        schema.add(':');
        schema.addRange('0', '9');
        schema.addRange('a', 'z');
        schema.addRange('A', 'Z');
        schema.print();
    }

    @Test public void testParser()
    {
        SimpleParser parser = new SimpleParser("  this = 21.7352672112  -.33333333");
        Assert.assertTrue(parser.literal("this"));
        Assert.assertTrue(parser.literal('='));
        Assert.assertEquals(21.7352672112d, parser.number(), 0.00000000001d);
        Assert.assertEquals(-.33333333d, parser.number(), 0.00000000001d);
    }
}