package com.clarisma.common.soar;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class Struct 
{
	private int location;
	private int size;
	private int anchorAndAlignment;
	private Struct next;
	
	public int location()
	{
		return location;
	}
	
	public void setLocation(int location)
	{
		this.location = location;
	}
	
	public int size()
	{
		return size;
	}
	
	protected void setSize(int size)
	{
		this.size = size;
	}
	
	public int anchor()
	{
		return anchorAndAlignment & 0x0fff_ffff;
	}
	
	public int anchorLocation()
	{
		return location() + anchor();
	}
	
	protected void setAnchor(int anchor)
	{
		assert anchor >= 0;
		assert anchor <= 0x0fff_ffff;
		anchorAndAlignment = (anchorAndAlignment & 0xf000_0000) | anchor;
	}
	
	public int alignment()
	{
		return anchorAndAlignment >>> 28;
	}
	
	public void setAlignment(int alignment)
	{
		assert alignment >= 0 && alignment < 16;
		anchorAndAlignment = (anchorAndAlignment & 0x0fff_ffff) | (alignment << 28);
	}
	
	public int alignedLocation(int pos)
	{
		int alignment = alignment();
		int alignBytes = 1 << alignment;
		int alignMask = 0xffff >> (16-alignment);
		return pos + ((alignBytes - (pos & alignMask)) & alignMask);
	}
	
	public Struct next()
	{
		return next;
	}
	
	public void setNext(Struct s)
	{
		next = s;
	}

	
	public Struct append(Struct s)
	{
		if(location > 0) 
		{
			int pos = alignedLocation(location + size);
			assert s.location == 0 || s.location == pos;
			s.location = pos;
		}
		next = s;
		return s;
	}

	public String dumped()
	{
		return toString();
	}
	
	public void dump(PrintWriter out)
	{
		out.format("%08X  %s\n", location(), dumped());
	}
	
	public abstract void writeTo(StructOutputStream out) throws IOException;

	public static <T extends Struct> T addToChain(T last, T s)
	{
		Struct sFirst = s.next();
		if (last != null)
		{
			s.setNext(last.next());
			if(sFirst == null)
			{
				last.setNext(s);
			}
			else
			{
				last.setNext(sFirst);
			}
		}
		else
		{
			if(sFirst == null) s.setNext(s);
		}
		return s;
	}
}
