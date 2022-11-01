/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.pbf;

public class PbfException extends RuntimeException 
{
	public PbfException (String msg)
	{
		super(msg);
	}

	public PbfException (String msg, Exception root)
	{
		super(msg, root);
	}
}
