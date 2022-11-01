/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.pbf;

import java.nio.ByteBuffer;

public class PbfEncoder 
{
	public static void writeVarint(ByteBuffer buf, long val)
	{
		while (val >= 0x80 || val < 0)
		{
			buf.put((byte)((val & 0x7f) | 0x80));
			val >>>= 7;
		}
		buf.put((byte)val);
	}

	public static int writeVarint(byte[] buf, int pos, long val)
	{
		int len = 1;
		while (val >= 0x80 || val < 0)
		{
			buf[pos] = (byte)((val & 0x7f) | 0x80);
			val >>>= 7;
			len++;
			pos++;
		}
		buf[pos] = (byte)val;
		return len;
	}

	public static int varintLength(int val)
	{
		if(val==0) return 1;
		return 5 - (Integer.numberOfLeadingZeros(val)+3) / 7;
	}
}
