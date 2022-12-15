package com.clarisma.common.text;

import com.clarisma.common.util.Log;
import org.junit.Test;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.*;

public class TableTest
{
    @Test public void testNumberFormat()
    {
        NumberFormat format = new DecimalFormat("####000,000,000.00### in");
        Log.debug(".123456789.123456789.123456789.123456789");
        Log.debug(format.format(50000.3));
    }

    @Test public void testTable() throws IOException
    {
        Table table = new Table();
        table.column();
        table.column().format("###,###,###,###.00 in");
        table.add("George");
        table.add(12);
        table.add("Apples & Bananas");
        table.add(1234.56);
        table.divider("-");
        table.add("A column with lots and lots of text");
        table.add("n/a");
        table.add("An even longer column that just keeps on and on and surely needs to be trimmed " +
            "so this table can be nicely displayed.");
        table.add("n/a");
        table.print(System.out);
    }
}