package com.geodesk.geom;

import java.util.List;

// not used
public class PairSort
{
	public static <T> void sort(int[] keys, List<T> values)
	{
		assert keys.length == values.size();
		sort(keys, values, 0, keys.length-1);
	}
	
	public static <T> void sort(int[] keys, List<T> values, int left, int right)
	{
		if (left >= right) return;

	    int pivot = keys[(left + right) >> 1];
	    int i = left - 1;
	    int j = right + 1;

	    for(;;)
	    {
	        do i++; while (keys[i] < pivot);
	        do j--; while (keys[j] > pivot);
	        if (i >= j) break;
	        swap(keys, values, i, j);
	    }

	    sort(keys, values, left, j);
	    sort(keys, values, j+1, right);
	}
	
	private static <T> void swap(int[] keys, List<T> values, int i, int j) 
	{
	    int tk = keys[i];
	    keys[i] = keys[j];
	    keys[j] = tk;
	    T tv = values.get(i);
	    values.set(i, values.get(j));
	    values.set(j, tv);
	}
}
