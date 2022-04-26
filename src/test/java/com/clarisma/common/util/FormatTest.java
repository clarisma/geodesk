package com.clarisma.common.util;

import com.clarisma.common.text.Format;
import org.junit.Test;

public class FormatTest
{
    @Test public void testFormatTimespan()
    {
        System.out.println(Format.formatTimespan(9));
        System.out.println(Format.formatTimespan(35 * 60 * 1000 + 42_000));
        System.out.println(Format.formatTimespan(4 * 60 * 60 * 1000 + 13 * 60 * 1000 + 42_000));
    }
}