/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 * This class is based on rbush (https://github.com/mourner/rbush)
 * by Volodymyr Agafonkin. The original work is licensed as follows:
 *
 * MIT License
 *
 * Copyright (c) 2016 Volodymyr Agafonkin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * (https://github.com/mourner/rbush/blob/master/LICENSE)
 */

package com.geodesk.geom;

import java.util.ArrayList;
import java.util.List;

// careful when translating code from JavaScript:
// must cast ints explicitly into doubles

// TODO: eliminate "stragglers": single item in one node, consolidate, especially
// if bounding box does not increase

public class OmtTreeBuilder<B extends Bounds> implements SpatialTreeBuilder<B>
{
	private final int maxEntries;
	private final SpatialTreeFactory<B> factory;

	public OmtTreeBuilder(SpatialTreeFactory<B> factory, int maxEntries)
	{
		this.factory = factory;
		this.maxEntries = maxEntries;
	}
	
	private B build(ArrayList<? extends Bounds> items, int left, int right, int height, int maxEntries)
	{
		int n = right - left + 1;
		int m = maxEntries;
		
		//log.debug("Building RTree at height {}, items {} to {}", height, left, right);

        if (n <= maxEntries) 
        {
            // reached leaf level; return leaf
            return factory.createLeaf(items, left, right+1);
        }

        if (height==0) 
        {
            // target height of the bulk-loaded tree
            height = (int)Math.ceil(Math.log(n) / Math.log(m));

            // target number of root entries to maximize storage utilization
            m = (int)Math.ceil((double)n / Math.pow(m, height - 1));
        }

	    List<B> children = new ArrayList<>();

	    // split the items into M mostly square tiles
	    
	    //log.debug("  m = {} (tile count)",  m);

	    int n2 = (int)Math.ceil((double)n / m);
	    int n1 = n2 * (int)Math.ceil(Math.sqrt(m));

	    QuickSelect.multiSelect(items, left, right, n1, OmtTreeBuilder::compareMinX);

	    for (int i = left; i <= right; i += n1) 
	    {
	    	int right2 = Math.min(i + n1 - 1, right);
	    	QuickSelect.multiSelect(items, i, right2, n2, OmtTreeBuilder::compareMinY);

	        for (int j = i; j <= right2; j += n2) 
	        {
	        	int right3 = Math.min(j + n2 - 1, right2);

	            // pack each entry recursively
	        	children.add(build(items, j, right3, height - 1, maxEntries));
	        }
		}
	    return factory.createBranch(children, 0, children.size());
	}
	
	
	private static int compareMinX(Bounds a, Bounds b) 
	{ 
		return a.minX() - b.minX(); 
	}
	
	private static int compareMinY(Bounds a, Bounds b) 
	{ 
		return a.minY() - b.minY(); 
	}

	@Override public B build(ArrayList<? extends Bounds> items)
	{
		return build(items, 0, items.size()-1, 0, maxEntries);
	}
}
