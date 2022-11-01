/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log
{
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss ");
    public static void log(String format, Object... args)
    {
        Date date = new Date();
        synchronized (System.out)
        {
            System.out.print(FORMAT.format(date));
            System.out.format(format, args);
            System.out.println();
        }
    }

    public static void debug(Object arg)
    {
        log("%s", arg);
    }

    public static void debug(String format, Object... args)
    {
        log(format, args);
    }

    public static void error(String format, Object... args)
    {
        log(format, args);  // TODO
    }

    public static void warn(String format, Object... args)
    {
        log(format, args);  // TODO
    }
}
