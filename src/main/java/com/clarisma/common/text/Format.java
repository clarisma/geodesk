/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.text;

public class Format
{
    public static String formatTimespan(long ms)
    {
        if(ms < 1000) return String.format("%dms", ms);
        if(ms < 60_000)
        {
            return String.format("%ds %dms", ms / 1000, ms % 1000);
        }
        if(ms < 60 * 60 * 1000)
        {
            long s = (ms+500) / 1000;
            return String.format("%dm %ds", s / 60, s % 60);
        }
        if(ms < 24 * 60 * 60 * 1000)
        {
            long m = (ms + 30*1000) / (60 * 1000);
            return String.format("%dh %dm", m / 60, m % 60);
        }
        long h = (ms + 30*60*1000) / (60 * 60 * 1000);
        return String.format("%dd %dh", h / 24, h % 24);
    }
}
