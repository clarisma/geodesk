/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import java.nio.ByteBuffer;

// TODO: See if we increase performance by scaling up bucket size; e.g.
//  start with 16, next bucket 32, etc.
//  initial benchmarks are inconclusive, change rolled back

// TODO: This could be an item in a circular list, which would get rid of the
//  `last` field (results are unsorted, so the order of `QueryResults` doesn't
//  matter (although object size would likely remain the same because of padding)

public class QueryResults
{
	final ByteBuffer buf;
	int[] pointers;
	int size;
	QueryResults next;
	private QueryResults last;	// only valid for first QueryResults in a chain
			
	private static final int DEFAULT_BUCKET_SIZE = 256;
	public static final QueryResults EMPTY = new QueryResults(null,0);

	public QueryResults(ByteBuffer buf, int maxSize)
	{
		this.buf = buf;
		pointers = new int[maxSize];
		last = this;
	}
	
	public QueryResults(ByteBuffer buf)
	{
		this(buf, DEFAULT_BUCKET_SIZE);
	}
	
	private QueryResults(ByteBuffer buf, int[] pointers, int size)
	{
		this.buf = buf;
		this.pointers = pointers;
		this.size = size;
	}
	
	public void add(int ptr)
	{
		if(size == pointers.length)
		{
			last = last.next = new QueryResults(buf, pointers, size);
			pointers = new int[pointers.length];
			size = 0;
		}
		pointers[size++] = ptr;
	}

	public static QueryResults merge(QueryResults a, QueryResults b)
	{
		if(a.size==0) return b;
		if(b.size==0) return a;
		a.last.next = b;
		a.last = b.last;
		return a;
	}
}
