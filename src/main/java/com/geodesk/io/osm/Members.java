/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io.osm;

import java.util.List;

import com.geodesk.feature.FeatureId;

import com.geodesk.feature.FeatureType;
import com.clarisma.common.pbf.PbfBuffer;


/**
 * A multi-field iterator for relation members.
 *
 */
public class Members 
{
	private PbfBuffer roleBuf;
	private PbfBuffer idBuf;
	private PbfBuffer typeBuf;
	private List<String> strings;
	private String role;
	private long id;
	private FeatureType type;
	
	public Members (List<String> strings, PbfBuffer roles, PbfBuffer ids, PbfBuffer types)
	{
		this.strings = strings;
		roleBuf = roles;
		idBuf = ids;
		typeBuf = types;
	}

	public boolean next()
	{
		if(!idBuf.hasMore()) return false;
		id += idBuf.readSignedVarint();	// delta-encoded
		role = strings.get((int)roleBuf.readVarint());
		type = FeatureType.values()[(int)typeBuf.readVarint()];
		return true;
	}

	public String role()
	{
		return role;
	}

	public long id()
	{
		return id;
	}

	public FeatureType type()
	{
		return type;
	}
	
	public long typedId()
	{
		return FeatureId.of(type, id);
	}

	public int size()
	{
		// TODO: will not count correctly if iteration has begun
		return idBuf.countVarInts();
	}

	/*
	// TODO: need to reset
	public void toArrays(long[] ids, String[] roles, byte[] types)
	{
		for(int i=0; i<ids.length; i++)
		{
			next();
			ids[i] = id;
			roles[i] = role;
			types[i] = (byte)type.ordinal();
		}
	}
	 */
}



