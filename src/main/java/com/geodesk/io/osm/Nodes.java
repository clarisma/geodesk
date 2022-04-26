package com.geodesk.io.osm;

import com.clarisma.common.pbf.PbfBuffer;

// TODO: ==> IdIterator
public class Nodes 
{
	private PbfBuffer buf;
	private long id = 0;
	
	public Nodes(PbfBuffer buf)
	{
		this.buf = buf;
		if (buf == null) buf = PbfBuffer.EMPTY;
	}

	public boolean next()
	{
		if(!buf.hasMore()) return false;
		id += buf.readSignedVarint();
		return true;
	}
	
	public long id()
	{
		return id;
	}
	
	public int size()
	{
		return buf.countVarInts();
	}

	/*
	// TODO: need to reset
	public long[] toArray()
	{
		long[] a = new long[size()];
		for(int i=0; next(); i++) a[i] = id;
		return a;
	}
	 */
}
