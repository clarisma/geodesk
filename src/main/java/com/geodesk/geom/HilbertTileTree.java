/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 *
 * This class is based on flatbush (https://github.com/mourner/flatbush)
 * by Volodymyr Agafonkin. The original work is licensed as follows:
 *
 * ISC License
 *
 * Copyright (c) 2018, Volodymyr Agafonkin
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 *
 * (https://github.com/mourner/flatbush/blob/master/LICENSE)
 */

package com.geodesk.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// @hidden
public class HilbertTileTree extends RTree
{
	private static class Pair implements Comparable<Pair>
	{
		int hilbertValue;
		Bounds item;
		
		public int compareTo(Pair o) 
		{
			return Integer.compare(hilbertValue, o.hilbertValue);
		}
	}
	
	public HilbertTileTree(List<Bounds> items, int zoom, int maxEntries)
	{
		Pair[] pairs = new Pair[items.size()];
		for(int i=0; i<pairs.length; i++)
		{
			Bounds b = items.get(i);
			int x = (b.centerX() >>> (32 - zoom - 15)) & 0x7fff;
			int y = (b.centerY() >>> (32 - zoom - 15)) & 0x7fff;
			Pair p = new Pair();
			p.hilbertValue = Hilbert.fromXY(x, y);
			p.item = b;
			pairs[i] = p;
			// log.debug("Hilbert {} for x/y {}/{}",  p.hilbertValue,  x,  y);
		}
		Arrays.sort(pairs);
		
		List<Node> children = new ArrayList<>();
		int start = 0;
		while(start < pairs.length)
		{
			int end = Integer.min(start+maxEntries, pairs.length);
			Node child = new Node(new ArrayList<Bounds>(end-start), true);
			for(int i=start; i<end; i++) child.add(pairs[i].item);
			children.add(child);
			start = end;
		}
		
		List<Node> parents = new ArrayList<>();
		for(;;)
		{
			start = 0;
			while(start < children.size())
			{
				int end = Integer.min(start+maxEntries, children.size());
				Node child = new Node(new ArrayList<Bounds>(end-start), false);
				for(int i=start; i<end; i++) child.add(children.get(i));
				parents.add(child);
				start = end;
			}	
			if(parents.size() == 1)
			{
				root = parents.get(0);
				return;
			}
			List<Node> temp = children;
			children = parents;
			parents = temp;
			parents.clear();
		}
	}

}
