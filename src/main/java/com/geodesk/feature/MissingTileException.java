/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

public class MissingTileException extends RuntimeException
{
    private int tip;

	public MissingTileException(int tip)
	{
		super(String.format("Missing tile: %06X", tip));
	}
}
