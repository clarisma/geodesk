package com.clarisma.common.pbf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

// TODO: unify these under an interface, so we can read ByteBuffer, byte array
//  and InputStream the same way

// TODO: maybe maintain start pos so we can reset the buffer?
public class PbfBuffer
{
	protected byte[] buf;
	protected int pos;
	protected int endPos;
	
	public static final PbfBuffer EMPTY = new PbfBuffer();

	public PbfBuffer()
	{
		buf = null;
		pos = 0;
		endPos = 0;
	}

	public PbfBuffer(byte[] data)
	{
		wrap(data);
	}

	public PbfBuffer(byte[] data, int start, int len)
	{
		buf = data;
		pos = start;
		endPos = start + len;
	}

	public void wrap(byte[] data)
	{
		buf = data;
		pos = 0;
		endPos = data.length;
	}

	public byte[] buf()
	{
		return buf;
	}
	
	public int pos()
	{
		return pos;
	}
	
	public int bytesRemaining()
	{
		return endPos - pos;
	}

	public int endPos()
	{
		return endPos;
	}
	
	public boolean load(InputStream in, int len) 
	{
		buf = new byte[len];
		pos = 0;
		endPos = len;
		try
		{
			int bytesRead = in.read(buf);
			return (bytesRead == len);
		}
		catch (IOException ex)
		{
			throw new PbfException("Unable to load buffer", ex);
		}
	}

	// TODO: does not respect the original window
	public void reset(byte[] data)
	{
		buf = data;
		pos = 0;
		endPos = data.length;
	}

	// TODO: may cause problems with sign extension, prefer (int)buf[pos++] & 0xFF ??
	public byte readByte() throws PbfException
	{
		try
		{
			return buf[pos++];
		}
		catch(ArrayIndexOutOfBoundsException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
	}
	
	public int readTag() throws PbfException
	{
		return (int)readVarint();
	}
	
	public long readVarint() throws PbfException
	{
		assert(pos < endPos);
		byte b;
		long val;
		try
		{
	        b = buf[pos++]; 
	        val = (b & 0x7f); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 7); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 14); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 21); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 28); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 35); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 42); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 49); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 56); 
	        if (b >= 0) return val;
	        b = buf[pos++]; 
	        val |= ((long)(b & 0x7f) << 63); 
	        if (b >= 0) return val;
			throw new PbfException("Bad VarInt format.");
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
    }
	
	/**
	 * Counts the number of varint values from the current buffer
	 * position until the end of the buffer. The current position
	 * remains unchanged. Useful when allocating an array prior
	 * to reading a series of values.
	 * 
	 * @return the number of varints
	 */
	public int countVarInts()
	{
		return countVarInts(pos);
	}

	public int countVarInts(int start)
	{
		int count = 0;
		for(int i=start; i<endPos; i++)
		{
			if (buf[i] >= 0) count++;
		}
		return count;
	}

	// TODO: can this fail if a non-canonical encoding is used?
	//  e.g. 0x81 0x00 instead of 0x01
	public int countVarIntsUntilZero(int start)
	{
		int count = 0;
		for(int i=start; i<endPos; i++)
		{
			if(buf[i] == 0) break;
			if (buf[i] >= 0) count++;
		}
		return count;
	}
	
	public long readSignedVarint () throws PbfException
	{
		long val = readVarint();
		return (val >> 1) ^ -(val & 1);
	}
	
	/*
	public float readFloat() throws PbfException
	{
		return Float.intBitsToFloat(bits)
	}
	*/
	
	public int readFixed32() throws PbfException
	{
		try
		{
			int val = (buf[pos] & 0xff) | 
				((buf[pos+1] & 0xff) << 8) |
				((buf[pos+2] & 0xff) << 16) |
				((buf[pos+3] & 0xff) << 24);
			pos += 4;
			return val;
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
	}

	public long readFixed64() throws PbfException
	{
		return (((long)readFixed32()) & 0xffff_ffffl) | 
			(((long)readFixed32()) << 32);
	}
	
	public float readFloat() throws PbfException
	{
		return Float.intBitsToFloat(readFixed32());
	}
	
	public double readDouble() throws PbfException
	{
		double d = Double.longBitsToDouble(readFixed64());
		// System.out.format("==> double %f\n", d);
		return d;
	}
	
	/**
	 * Reads a 32-bit integer in network byte order
	 * (big endian) -- this is not how fixed 32-bit values
	 * are encoded, use readFixed32 instead
	 * 
	 * TODO: move out of this class, used only
	 * by OsmReader
	 * 
	 * @throws PbfException
	 */
	public int readFixedIntNBO() throws PbfException
	{
		try
		{
			int val = (buf[0]<<24) | (buf[1]<<16) | (buf[2]<<8 ) | buf[3];
			pos += 4;
			return val;
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			throw new PbfException("Attempt to read past end of buffer.");
		}
	}
	
	/*// TODO: may be broken
	public long readFixedLong() throws PbfException
	{
		return (((long)readFixedInt()) << 32) | readFixedInt();
	}
	*/
	
	public String readString() throws PbfException
	{
		int len = (int)readVarint();
		return readString(len);
	}

	public String readString(int len) throws PbfException
	{
		String val;
		try
		{
			val = new String(buf, pos, len, "UTF-8");
			// System.out.println("  String: " + val);
		}
		catch (Exception ex)
		{
			throw new PbfException("Unable to read string.", ex);
		}
		pos += len;
		return val;
	}
	
	public boolean hasMore()
	{
		return pos < endPos;
	}
	
	public void skip(int len)
	{
		pos += len;
	}
	
	public void skipEntity(int marker)
	{
		//System.out.println("Skipping entity #" + (marker >>> 3));
		switch (marker & 7)
		{
		case PbfType.VARINT:
			readVarint();
			break;
		case PbfType.FIXED64:
			skip(8);
			break;
		case PbfType.STRING:
			int len = (int)readVarint();
			skip(len);
			break;
		case PbfType.FIXED32:
			skip(4);
			break;
		default:
			throw new PbfException("Unknown type: " + (marker & 7));
		}
	}
	
	public void end()
	{
		pos = endPos;
	}
	
	public void dump(int from, int to)
	{
		StringBuilder b = new StringBuilder();
		StringBuilder bHex = new StringBuilder();
		for(int i=from; i<buf.length && i<to; i++)
		{
			b.append(" ");
			b.append(String.valueOf(buf[i] & 0xff));
			bHex.append(" ");
			bHex.append(Integer.toHexString(buf[i] & 0xff));
		}
		System.out.println("Remaining bytes: " + b.toString());
		System.out.println("         as hex: " + bHex.toString());
	}
	
	public PbfBuffer readMessage()
	{
		int len = (int)readVarint();
		PbfBuffer msg = new PbfBuffer(buf, pos, len);
		pos += len;
		return msg;
	}
	
	public void seek(int newPos)
	{
		pos = newPos;
	}

	public int peek()
	{
		if (pos < endPos) return buf[pos] &0xff;
		return 0;
	}

	public int getByte(int p)
	{
		return buf[p] &0xff;
	}
}
