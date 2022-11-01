/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io.osm;

import com.geodesk.feature.Tags;

import java.util.Collections;
import java.util.Map;

public class EmptyTags implements Tags
{
	@Override public boolean isEmpty()
	{
		return true;
	}

	@Override public int size()
	{
		return 0;
	}

	@Override public boolean next()
	{
		return false;
	}

	@Override public String key()
	{
		return "";
	}

	@Override public Object value()
	{
		return "";
	}

	@Override public String stringValue()
	{
		return "";
	}

	@Override public int intValue()
	{
		return 0;
	}

	@Override public Map<String, Object> toMap()
	{
		return Collections.emptyMap();
	}
}
