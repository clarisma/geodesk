/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.core.Box;

public class BoundedItem<T> extends Box
{
	private T item;
	
	public BoundedItem(Bounds b, T item)
	{
		super(b);
		this.item = item;
	}
	
	public T get()
	{
		return item;
	}
}
