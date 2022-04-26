package com.geodesk.io.osm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geodesk.feature.Tags;
import com.geodesk.feature.store.TagValues;
import com.clarisma.common.pbf.PbfBuffer;

public class KeyValueTags implements Tags
{
	private PbfBuffer keyBuf;
	private PbfBuffer valueBuf;
	private int keyBufStartPos;
	private int valueBufStartPos;
	private String key;
	private String value;
	private List<String> strings;
	
	public KeyValueTags (List<String> strings, PbfBuffer keys, PbfBuffer values)
	{
		this.strings = strings;
		keyBuf = keys;
		valueBuf = values;
		keyBufStartPos = keys.pos();
		valueBufStartPos = values.pos();
	}

	@Override public boolean isEmpty()
	{
		return keyBuf.endPos() - keyBufStartPos > 0;
	}

	@Override public int size()
	{
		return keyBuf.countVarInts(keyBufStartPos);
	}

	@Override public boolean next()
	{
		if(!keyBuf.hasMore()) return false;
		int index = (int)keyBuf.readVarint();
		key = strings.get(index);
		index = (int)valueBuf.readVarint();
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

	@Override public int intValue()
	{
		return TagValues.toInt(value);
	}

	// TODO: does not preserve iterator pos
	@Override public Map<String, Object> toMap()
	{
		Map<String, Object> map = new HashMap<>();
		keyBuf.seek(keyBufStartPos);
		valueBuf.seek(valueBufStartPos);
		while (next()) map.put(key(), value());
		return map;
	}
}
