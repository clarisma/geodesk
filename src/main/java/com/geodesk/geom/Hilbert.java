/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 *
 * This class is based on flatbush (https://github.com/mourner/flatbush)
 * by Vladimir Agafonkin. The original work is licensed as follows:
 *
 * ISC License
 *
 * Copyright (c) 2018, Vladimir Agafonkin
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
 *
 * The original work is based on Fast Hilbert curve algorithm by http://threadlocalmutex.com,
 * ported from C++ https://github.com/rawrunprotected/hilbert_curves (public domain).
 */

package com.geodesk.geom;

public class Hilbert
{
	/**
	 * Calculates the distance of a coordinate along the Hilbert Curve. 
	 * 
	 * The coordinate space is technically 0 <= x/y < 2^16, but this would require 
	 * treating the signed result as an unsigned value. Since this is unintuitive and
	 * leads to needless frustrations, the maximum coordinate value should be 2^15-1.
	 * Technically, this means we use a single quadrant of a 16th-order Hilbert curve
	 * (i.e. a 15th-order Hilbert curve).
	 * 
	 * TODO: could we just use longs instead?
	 *  
	 * @param x (must be 0 <= x < 2^15)
	 * @param y (must be 0 <= y < 2^15)
	 * @return the Hilbert Curve distance; (0 <= distance < 2^30)
	 */
	public static int fromXY(int x, int y)
	{
		assert x >= 0 && x < (1 << 15): String.format("%d is out of range", x);
		assert y >= 0 && y < (1 << 15): String.format("%d is out of range", x);
		
		/*
		x = x << 16;
		y = y << 16;
		*/
		
		int a = x ^ y;
		int b = 0xFFFF ^ a;
		int c = 0xFFFF ^ (x | y);
		int d = x & (y ^ 0xFFFF);

		int A = a | (b >> 1);
		int B = (a >> 1) ^ a;
		int C = ((c >> 1) ^ (b & (d >> 1))) ^ c;
		int D = ((a & (c >> 1)) ^ (d >> 1)) ^ d;

		a = A; b = B; c = C; d = D;
		A = ((a & (a >> 2)) ^ (b & (b >> 2)));
		B = ((a & (b >> 2)) ^ (b & ((a ^ b) >> 2)));
		C ^= ((a & (c >> 2)) ^ (b & (d >> 2)));
		D ^= ((b & (c >> 2)) ^ ((a ^ b) & (d >> 2)));

		a = A; b = B; c = C; d = D;
		A = ((a & (a >> 4)) ^ (b & (b >> 4)));
		B = ((a & (b >> 4)) ^ (b & ((a ^ b) >> 4)));
		C ^= ((a & (c >> 4)) ^ (b & (d >> 4)));
		D ^= ((b & (c >> 4)) ^ ((a ^ b) & (d >> 4)));

		a = A; b = B; c = C; d = D;
		C ^= ((a & (c >> 8)) ^ (b & (d >> 8)));
		D ^= ((b & (c >> 8)) ^ ((a ^ b) & (d >> 8)));
		
		a = C ^ (C >> 1);
		b = D ^ (D >> 1);

		int i0 = x ^ y;
		int i1 = b | (0xFFFF ^ (i0 | a));

		i0 = (i0 | (i0 << 8)) & 0x00FF00FF;
		i0 = (i0 | (i0 << 4)) & 0x0F0F0F0F;
		i0 = (i0 | (i0 << 2)) & 0x33333333;
		i0 = (i0 | (i0 << 1)) & 0x55555555;
		
		i1 = (i1 | (i1 << 8)) & 0x00FF00FF;
		i1 = (i1 | (i1 << 4)) & 0x0F0F0F0F;
		i1 = (i1 | (i1 << 2)) & 0x33333333;
		i1 = (i1 | (i1 << 1)) & 0x55555555;
		
		// return ((i1 << 1) | i0) >>> (32 - 2 * zoom);
		return (i1 << 1) | i0;
	}
	
	public static void main(String args[])
	{
		int x = 0;
		int y = 0;
		int zoom = 16;
		
		System.out.format("%d\n", fromXY(x,y));
		x = 50;
		y = 100;
		System.out.format("%d\n", fromXY(x,y));
		x = y = 8000;
		System.out.format("%d\n", fromXY(x,y));
		x = y = 8500;
		System.out.format("%d\n", fromXY(x,y));
		x = y = (1 << 14)-1;
		System.out.format("%d\n", fromXY(x,y));
		x = (1 << 15)-1;
		y = (1 << 16)-1;
		System.out.format("%d\n", fromXY(x,y));
		x = (1 << 16)-1;
		y = 1;
		System.out.format("%d\n", fromXY(x,y));
		x = 0;
		y = (1 << 16)-1;
		System.out.format("%d\n", fromXY(x,y));
		x = -(1 << 1)+1;
		y = -(1 << 15)+1;
		System.out.format("%d\n", fromXY(x,y));
		x = 0;
		y = (1 << 15)-1;
		System.out.format("%d\n", fromXY(x,y));
		x = (1 << 15)-1;
		y = (1 << 15);
		System.out.format("%d\n", fromXY(x,y));
	}
}
