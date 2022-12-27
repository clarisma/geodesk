/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.pbf;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

// static methods are not threadsafe

// TODO: unify these under an interface, so we can read ByteBuffer, byte array
//  and InputStream the same way

public class PbfDecoder 
{
	private final ByteBuffer buf;
	private int pos;

	/*
	public PbfDecoder()
	{
		// TODO: this makes no sense
	}
	 */
	
	public PbfDecoder(ByteBuffer buf, int pos)
	{
		this.buf = buf;
		this.pos = pos;
	}
	
	public int pos()
	{
		return pos;
	}
	
	public long readVarint() throws PbfException
	{
		byte b;
		long val;
		try
		{
	        b = buf.get(pos++); 
	        val = (b & 0x7f); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 7); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 14); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 21); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 28); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 35); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 42); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 49); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 56); 
	        if (b >= 0) return val;
	        b = buf.get(pos++); 
	        val |= ((long)(b & 0x7f) << 63); 
	        if (b >= 0) return val;
			throw new PbfException("Bad VarInt format.");
		}
		catch (BufferUnderflowException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
    }
	
	public static long readVarint(ByteBuffer buf) throws PbfException
	{
		byte b;
		long val;
		try
		{
	        b = buf.get(); 
	        val = (b & 0x7f); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 7); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 14); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 21); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 28); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 35); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 42); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 49); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 56); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 63); 
	        if (b >= 0) return val;
			throw new PbfException("Bad VarInt format.");
		}
		catch (BufferUnderflowException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
    }
	
	public static long readVarint(InputStream in) throws IOException, PbfException
	{
		byte b;
		long val;
	    b = (byte)in.read(); 
        val = (b & 0x7f); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 7); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 14); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 21); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 28); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 35); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 42); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 49); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 56); 
        if (b >= 0) return val;
	    b = (byte)in.read(); 
        val |= ((long)(b & 0x7f) << 63); 
        if (b >= 0) return val;
		throw new PbfException("Bad VarInt format.");
    }
	
	public static long readSignedVarint (InputStream in) throws IOException
	{
		long val = readVarint(in);
		return (val >> 1) ^ -(val & 1);
	}
	
	public static int readVarintSmall(ByteBuffer buf) throws PbfException
	{
		byte b;
		int val;
		try
		{
	        b = buf.get(); 
	        val = (b & 0x7f); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 7); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 14); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 21); 
	        if (b >= 0) return val;
	        b = buf.get(); 
	        val |= ((long)(b & 0x7f) << 28); 
	        if (b >= 0) return val;
			throw new PbfException("Bad VarInt format.");
		}
		catch (BufferUnderflowException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
    }
	
	public long readSignedVarint () throws PbfException
	{
		long val = readVarint();
		return (val >> 1) ^ -(val & 1);
	}
	
	public static long readSignedVarint (ByteBuffer buf) throws PbfException
	{
		long val = readVarint(buf);
		return (val >> 1) ^ -(val & 1);
	}
	
	public String readRawString(int len) throws UnsupportedEncodingException
	{
		buf.position(pos);
		byte[] bytes = new byte[len];
		buf.get(bytes);
		pos += len;
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public String readString()
	{
		int len = (int)readVarint();
		byte[] bytes = new byte[len];
		for(int i=0; i<len; i++)
		{
			bytes[i] = buf.get(pos++);
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public int readFixed32()
	{
		int v = buf.getInt(pos);
		pos += 4;
		return v;
	}

	public byte readByte()
	{
		byte b = buf.get(pos);
		pos++;
		return b;
	}

	public static String readString(ByteBuffer buf) throws PbfException
	{
		int len = (int)readVarint(buf);
		byte[] bytes = new byte[len];
		buf.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	// TODO: improve
	public boolean equalsString(String s)
	{
		/*
		int len = s.length();
		if((int)readVarint() != len) return false;
		*/
		return readString().equals(s);
	}

	public void skip(int len)
	{
		pos += len;
	}

	public void seek(int newPos)
	{
		pos = newPos;
	}

	public boolean hasMore()
	{
		return pos < buf.limit();
	}

	public ByteBuffer buf()
	{
		return buf;
	}
}
