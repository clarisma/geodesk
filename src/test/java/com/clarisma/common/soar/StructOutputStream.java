package com.clarisma.common.soar;

import com.clarisma.common.pbf.PbfOutputStream;
import com.clarisma.common.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class StructOutputStream extends OutputStream
{
	private OutputStream out;
	private int pos;
	private PbfOutputStream links;
	
	public StructOutputStream(OutputStream out) 
	{
		this.out = out;
	}

	public void setLinks(PbfOutputStream links)
	{
		this.links = links;
	}
	
	public void writeVarint(long val) throws IOException
	{
		while (val >= 0x80 || val < 0)
		{
			write ((int)(val & 0x7f) | 0x80);
			val >>>= 7;
		}
		write((int)val);
	}
	
	public void write(int b) throws IOException
	{
		out.write(b);
		pos++;
	}
	
    public void write(byte b[]) throws IOException 
    {
        out.write(b);
        pos += b.length;
    }
    
    public void write(byte b[], int off, int len) throws IOException 
    {
        out.write(b, off, len);
        pos += len;
    }   
    
    public void writeInt(int v) throws IOException 
    {
        out.write((v >>>  0) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
        pos += 4;
    }
    
    public void writeShort(int v) throws IOException 
    {
    	out.write((v >>>  0) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        pos += 2;
    }
    
    public void writeLong(long v) throws IOException 
    {
        writeInt((int)v);
        writeInt((int)(v >> 32));
    }

	
	public int position()
	{
		return pos;
	}

	public void writePointer(Struct target) throws IOException
	{
		if(target == null)
		{
			// TODO: warn
			writeInt(0); 
			return;
		}
		writeInt(target.anchorLocation() - pos);
	}

	public void writePointer(Struct target, int flags) throws IOException
	{
		int p;
		if(target == null)
		{
			// TODO: warn
			p = 0; 
		}
		else
		{
			p = target.anchorLocation() - pos;
		}
		// TODO: check if flags clash with pointer?
		writeInt(p | flags);
	}

	// deprecated
	public void writeShiftedPointer(Struct target, int flags) throws IOException
	{
		int p;
		if(target == null)
		{
			// TODO: warn
			p = 0; 
		}
		else
		{
			p = target.anchorLocation() - pos;
		}
		// TODO: check if flags clash with pointer?
		writeInt((p << 1) | flags);
	}

	// TODO: not all tagged pointers are shifted; if 2-byte aligned and we only need one
	// flag, no need to shift; if 4-byte aligned and we only need two flags, no need for
	// shift
	public void writeTaggedPointer(Struct target, int flagCount, int flags) throws IOException
	{
		assert flagCount > 0: "No need for tagged pointer if flagCount = 0";
		int p;
		if(target == null)
		{
			// TODO: warn
			p = 0;
		}
		else
		{
			// TODO: This is wrong -- make caller request specific treatment
			//  of pointer, don't arbitrarily rebase it
			//  This messes up pointers to strings, which are normally
			//  1-byte aligned, but change to 4-byte alignment if they are
			//  used as keys due to the constraints of tag tables

			// TODO: current code may be ok, but better to make explicit

			if(target.location() == 0)
			{
				Log.debug("Target {} has not been placed", target);
			}

			int alignment = target.alignment();
			assert (flagCount-1) <= alignment:
				String.format("Cannot have %d flag bits for a pointer to a 2^%d aligned struct",
					flagCount, alignment);
			int from = pos & (0xffff_ffff << (flagCount-1));
			p = target.anchorLocation() - from;

			/*
			assert p == (p & (0xffff_ffff << alignment)):
				String.format("Pointer %X is not aligned to 2^%d", p, alignment);
			 */
		}

		assert flags == (flags & (0xffff_ffff >> (32-flagCount))):
			String.format("Flags must only use the lowest %d bits", flagCount);

		writeInt((p << 1) | flags);
	}

	public void write(Struct s) throws IOException
	{
		assert pos <= s.location():
			String.format("%s cannot be placed at %d, since it must be at or after %d",
				s, s.location(), pos);
		while(pos < s.location()) write(0);
		int oldPos = pos;
		s.writeTo(this);
		assert pos == oldPos + s.size(): 
			String.format("Size of %s is %d, but %d bytes were written",
				s, s.size(), pos-oldPos);
	}
	
	public void writeBlank(int len) throws IOException
	{
		for(int i=0; i<len; i++) write(0);
	}
	
	
	public void writeChain(Struct s) throws IOException
	{
		while(s != null)
		{
			write(s);
			s = s.next();
		}
	}

    public void writeForeignPointer(int tile, long id, int shift, int flags) throws IOException
    {
		assert links != null;
		assert shift >= 0 && shift <= 4;
		links.writeFixed32(pos);
		links.writeFixed32((tile << 4) | shift);
		links.writeFixed64(id);
		writeInt(flags);
    }
}
