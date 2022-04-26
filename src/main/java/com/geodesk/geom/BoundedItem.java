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
