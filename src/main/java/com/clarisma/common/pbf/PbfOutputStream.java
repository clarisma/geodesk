/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.pbf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PbfOutputStream extends ByteArrayOutputStream
{
	// check encoding of negative varints
	public void writeVarint(long val)
	{
		while (val >= 0x80 || val < 0)		// TODO: improve check?
		{
			write ((int)(val & 0x7f) | 0x80);
			val >>>= 7;
		}
		write((int)val);
	}
	
	public void writeSignedVarint(long val)
	{
		writeVarint((val << 1) ^ (val >> 63));
	}
	
	public void writeString(String val)
	{
		byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
		writeVarint(bytes.length);
		writeBytes(bytes);
	}
	
	public void writeString (PbfOutputStream other)
	{
		writeVarint(other.count);
		write(other.buf, 0, other.count);
	}
	
	public void writeString (byte[] bytes)
	{
		writeVarint(bytes.length);
		write(bytes, 0, bytes.length);
	}
	
	public void writeString (byte[] bytes, int start, int len)
	{
		writeVarint(len);
		write(bytes, start, len);
	}
	
	public void writeMessage(int tag, ByteArrayOutputStream other)
	{
		writeVarint(tag);
		writeVarint(other.size());
		try 
		{
			other.writeTo(this);
		} 
		catch (IOException ex) 
		{
			throw new PbfException("Writing failed.", ex);
		}
	}
	
	public void writeFixed32(int val)
	{
		write(val & 0xff);
		write((val >>> 8) & 0xff);
		write((val >>> 16) & 0xff);
		write((val >>> 24) & 0xff);
	}
	
	public void writeFixed64(long val)
	{
		writeFixed32((int)val);
		writeFixed32((int)(val >>> 32));
	}
	
	// TODO: check
	public void writeFloat(float val)
	{
		writeFixed32(Float.floatToRawIntBits(val));
	}
	
	// TODO: check
	public void writeDouble(double val)
	{
		writeFixed64(Double.doubleToRawLongBits(val));
	}
	
	public synchronized void writeTo(ByteBuffer out)
	{
		out.put(buf, 0, count);
	}
	
	public byte[] buffer()
	{
		return buf;
	}
}
