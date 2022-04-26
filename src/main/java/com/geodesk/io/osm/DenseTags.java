package com.geodesk.io.osm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geodesk.feature.Tags;
import com.clarisma.common.pbf.PbfBuffer;

// TODO: This class is vulnerable if client calls next() after next() has already
//  returned false

// TODO: This entire class is not very robust, redesign

public class DenseTags implements Tags
{
	private PbfBuffer buf;
	private int start;
	private String key;
	private String value;
	private List<String> strings;

	public DenseTags (List<String> strings, PbfBuffer buf)
	{
		this.buf = buf;
		this.strings = strings;
		start = buf.pos();
	}

	@Override public boolean isEmpty()
	{
		return start == buf.endPos() || buf.getByte(start) == 0;
	}

	@Override public int size()
	{
		return buf.countVarIntsUntilZero(start);
	}

	@Override public boolean next()
	{
		if(!buf.hasMore()) return false;	// buf may be empty if none of the nodes have tags
		int index = (int)buf.readVarint();
		if(index == 0) return false;
		key = strings.get(index);
		index = (int)buf.readVarint();
		value = strings.get(index);
		return true;
	}

	@Override public String key()
	{
		return key;
	}

	@Override public Object value()
	{
		return value;
	}

	@Override public String stringValue()
	{
		return value;
	}

	// TODO: does not maintain current pos
	@Override public Map<String, Object> toMap()
	{
		buf.seek(start);
		Map<String, Object> map = new HashMap<>();
		while (next()) map.put(key(), value());
		return map;
	}

	// TODO: this is hacky, redesign
	public void advanceGroup()
	{
		if (buf.pos() == start) buf.readVarint();
		start = buf.pos();
	}
}
