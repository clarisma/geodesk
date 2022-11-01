/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

public class QueryException extends RuntimeException
{
	public QueryException(String msg)
	{
		super(msg);
	}
	
	public QueryException(String msg, Exception ex)
	{
		super(msg, ex);
	}
}
