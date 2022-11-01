/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import java.util.List;

// careful when translating code from JavaScript:
// must cast ints explicitly into doubles

// TODO: eliminate "stragglers": single item in one node, consolidate, especially
// if bounding box does not increase

public class OverlapMinimizingTree extends RTree 
{
	public OverlapMinimizingTree(List<Bounds> items, int maxEntries)
	{
		root = build(items, 0, items.size()-1, 0, maxEntries);
	}
	
	private Node build(List<Bounds> items, int left, int right, int height, int maxEntries)
	{
		int n = right - left + 1;
		int m = maxEntries;
		
		//log.debug("Building RTree at height {}, items {} to {}", height, left, right);

        if (n <= maxEntries) 
        {
            // reached leaf level; return leaf
            return new Node(items.subList(left, right+1), true);
        }

        if (height==0) 
        {
            // target height of the bulk-loaded tree
            height = (int)Math.ceil(Math.log(n) / Math.log(m));

            // target number of root entries to maximize storage utilization
            m = (int)Math.ceil((double)n / Math.pow(m, height - 1));
        }

	    Node node = new Node(null, false);

	    // split the items into M mostly square tiles
	    
	    //log.debug("  m = {} (tile count)",  m);

	    int n2 = (int)Math.ceil((double)n / m);
	    int n1 = n2 * (int)Math.ceil(Math.sqrt(m));

	    QuickSelect.multiSelect(items, left, right, n1, OverlapMinimizingTree::compareMinX);

	    for (int i = left; i <= right; i += n1) 
	    {
	    	int right2 = Math.min(i + n1 - 1, right);
	    	QuickSelect.multiSelect(items, i, right2, n2, OverlapMinimizingTree::compareMinY);

	        for (int j = i; j <= right2; j += n2) 
	        {
	        	int right3 = Math.min(j + n2 - 1, right2);

	            // pack each entry recursively
	        	node.add(build(items, j, right3, height - 1, maxEntries));
	        }
		}
	    return node;
	}
	
	
	private static int compareMinX(Bounds a, Bounds b) 
	{ 
		return a.minX() - b.minX(); 
	}
	
	private static int compareMinY(Bounds a, Bounds b) 
	{ 
		return a.minY() - b.minY(); 
	}
}
