/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 * This class is based on QuickSelect (https://github.com/mourner/quickselect)
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
 * (https://github.com/mourner/quickselect/blob/master/LICENSE)
 */

package com.geodesk.geom;

import java.util.Comparator;
import java.util.List;

import org.eclipse.collections.impl.stack.mutable.primitive.IntArrayStack;

/// @hidden
public class QuickSelect
{

	// sort an array so that items come in groups of n unsorted items, with groups sorted between each other;
	// combines selection algorithm with binary divide & conquer approach

	public static <T> void multiSelect(List<T> arr, int left, int right, int n, Comparator<T> compare) 
	{
	    IntArrayStack stack = new IntArrayStack();	  
	    stack.push(left);
	    stack.push(right);
	    
	    //log.debug("QuickSelect: Sorting array from {} to {}, n = {}", left ,  right, n);

	    while (!stack.isEmpty()) 
	    {
	        right = stack.pop();
	        left = stack.pop();

	        if (right - left <= n) continue;

	        int mid = left + (int)Math.ceil((double)(right - left) / n / 2) * n;
	        // log.debug("  mid = {}", mid);
	        QuickSelect.quickselect(arr, mid, left, right, compare);

	        stack.push(left);
	        stack.push(mid);
	        stack.push(mid);
	        stack.push(right);
	    }
	}
	
	public static <T> void quickselect(List<T> arr, int k, int left, int right, Comparator<T> compare) 
	{
	    quickselectStep(arr, k, left, right != 0 ? right : (arr.size() - 1), compare);
	}

	private static <T> void quickselectStep(List<T> arr, int k, int left, int right, Comparator<T> compare) 
	{
	    while (right > left) 
	    {
	    	//log.debug("  quickselectStep: {} to {}, k = {}", left, right, k);
	    	
	        if (right - left > 600) 
	        {
	            double n = right - left + 1;
	            double m = k - left + 1;
	            double z = Math.log(n);
	            double s = 0.5 * Math.exp(2 * z / 3);
	            double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * (m - n / 2 < 0 ? -1 : 1);
	            int newLeft = Math.max(left, (int)Math.floor(k - m * s / n + sd));
	            int newRight = Math.min(right, (int)Math.floor(k + (n - m) * s / n + sd));
	            quickselectStep(arr, k, newLeft, newRight, compare);
	        }

	        T t = arr.get(k);
	        int i = left;
	        int j = right;

	        swap(arr, left, k);
	        if (compare.compare(arr.get(right), t) > 0) swap(arr, left, right);

	        while (i < j) 
	        {
	            swap(arr, i, j);
	            i++;
	            j--;
	            while (compare.compare(arr.get(i), t) < 0) i++;
	            while (compare.compare(arr.get(j), t) > 0) j--;
	        }

	        if (compare.compare(arr.get(left), t) == 0) 
	        {
	        	swap(arr, left, j);
	        }
	        else 
	        {
	            j++;
	            swap(arr, j, right);
	        }
	        if (j <= k) left = j + 1;
	        if (k <= j) right = j - 1;
	    }
	}

	private static <T> void swap(List<T> arr, int i, int j) 
	{
	    T tmp = arr.get(i);
	    arr.set(i, arr.get(j));
	    arr.set(j, tmp);
	}

}
