/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

// TODO: needed?
/// @hidden
public class FeatureException extends RuntimeException
{
	public FeatureException(String msg)
	{
		super(msg);
	}
	
	public FeatureException(String msg, Exception ex)
	{
		super(msg, ex);
	}
}
