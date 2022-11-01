/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.core.Mercator;

/**
 * A bounding box that is able to represent an axis-aligned rectangle, even
 * if it crosses the antimeridian.
 */
// TODO: much simpler: if minX > maxX, we're crossing the 180

public class Box_alt
{
    private int minY;
    private int maxY;
    private int left;
    private int width;

    public Box_alt()
    {
        setToNull();
    }

    public Box_alt(Box_alt other)
    {
        setTo(other);
    }

    private Box_alt(int left, int width, int minY, int maxY)
    {
        this.left = left;
        this.width = width;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean isNull()
    {
        return maxY < minY;
    }

    public void setToNull()
    {
        left = 0;
        width = 0;
        minY = 0;
        maxY = -1;
    }

    public long width()
    {
        return minY > maxY ? 0 : (width & 0xffff_ffffL) + 1;
    }

    public long height()
    {
        return maxY - minY + 1;
    }

    public long area()
    {
        return width() * height();
    }

    public int north()
    {
        return maxY;
    }

    public int south()
    {
        return maxY;
    }

    public int west()
    {
        return left;
    }

    public int east()
    {
        return left+width;
    }

    public boolean crossesAntimeridian()
    {
        return (left+width) < left;
    }

    public boolean contains(int x, int y)
    {
        if (y < minY || y > maxY) return false;
        int right = left + width;
        if(left <= right)
        {
            return x >= left && x <= right;
        }
        else
        {
            return x >= left || x <= right;
        }
    }

    public boolean intersects(Box_alt other)
    {
        if (other.maxY < minY || other.minY > maxY) return false;
        int right = left + width;
        int otherRight = other.left + other.width;
        if(left <= right)
        {
            if(other.left <= otherRight)
            {
                return right >= other.left && left <= otherRight;
            }
            else
            {
                return right >= other.left || left <= otherRight;
            }
        }
        else
        {
            if(other.left <= otherRight)
            {
                return right >= other.left && left <= otherRight;
            }
            return true;
        }
    }

    // TODO: does not work if crossing 180
    public void expandToInclude(int x, int y)
    {
        if(isNull())
        {
            left = x;
            width = 0;
            minY = maxY = y;
            return;
        }
        if (y < minY)
        {
            minY = y;
        }
        else if (y > maxY)
        {
            maxY = y;
        }
        if (x < left)
        {
            width += left - x;
            left = x;
        }
        else
        {
            int extra = x - (left + width);
            if (extra > 0) width += extra;
        }
    }

    public void expandToInclude(Bounds b)
    {
        expandToInclude(b.minX(), b.maxX(), b.minY(), b.maxY());
    }

    private void expandToInclude(int otherMinX, int otherMaxX, int otherMinY, int otherMaxY)
    {
        if(isNull())
        {
            left = otherMinX;
            width = otherMaxX-otherMinX;
            minY = otherMinY;
            maxY = otherMaxY;
            return;
        }
        if (otherMinY < minY) minY = otherMinY;
        if (otherMaxY > maxY) maxY = otherMaxY;
        if (otherMinX < left)
        {
            width += left - otherMinX;
            left = otherMinX;
        }
        int add = otherMaxX - left + width;
        if (add > 0)
        {
            width += add;
        }
    }

    private void setTo(Box_alt other)
    {
        left = other.left;
        width = other.width;
        minY = other.minY;
        maxY = other.maxY;
    }

    public void expandToInclude(Box_alt other)
    {
        if(isNull())
        {
            setTo(other);
            return;
        }
        int otherLeft = other.left;
        int otherWidth = other.width;
        int otherMinY = other.minY;
        int otherMaxY = other.maxY;
        if (otherMinY < minY) minY = otherMinY;
        if (otherMaxY > maxY) maxY = otherMaxY;
        if (otherLeft < left)
        {
            width += left - otherLeft;
            left = otherLeft;
        }
        if ((otherWidth & 0xffff_ffffL) > (width & 0xffff_ffffL))
        {
            width = otherWidth;
        }
    }

    public void buffer(int delta)
    {
        minY = (int) Math.max((long) minY - delta, Integer.MIN_VALUE);
        maxY = (int) Math.min((long) maxY + delta, Integer.MAX_VALUE);

        long oldWidth = width & 0xffff_ffffL;
        long newWidth = Math.min(oldWidth + delta + delta, 0xffff_ffffL);
        left -= (int) ((newWidth - oldWidth) >> 1);
        width = (int) newWidth;
        if (delta < 0)
        {
            if (minY > maxY || newWidth < 0) setToNull();
        }
    }

    public void translate(int deltaX, int deltaY)
    {
        left += deltaX;
        if (deltaY > 0)
        {
            minY = trimmedAdd(minY, deltaY);
            maxY = trimmedAdd(maxY, deltaY);
        }
        else
        {
            minY = trimmedSubtract(minY, deltaY);
            maxY = trimmedSubtract(maxY, deltaY);
        }
    }

    @Override
    public String toString()
    {
        return isNull() ? "[empty]" :
            String.format("[%d,%d -> %d,%d]", left, minY, left+width, maxY);
    }


    /**
     * Overflow-safe subtraction
     *
     * @param x
     * @param y
     * @return the result of the subtraction; or the lowest negative value in case of an overflow
     */
    private static int trimmedSubtract(int x, int y)
    {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) return Integer.MIN_VALUE;
        return r;
    }

    /**
     * Overflow-safe addition
     *
     * @param x
     * @param y
     * @return the result of the addition; or the highest positive value in case of an overflow
     */
    private static int trimmedAdd(int x, int y)
    {
        int r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) return Integer.MAX_VALUE;
        return r;
    }

    // TODO: rounding before (int)
    public static Box_alt ofLonLatLonLat(double lon1, double lat1, double lon2, double lat2)
    {
        int minX = (int) Mercator.xFromLon(lon1);
        int maxX = (int) Mercator.xFromLon(lon2);
        int minY = (int) Mercator.yFromLat(lat1);
        int maxY = (int) Mercator.yFromLat(lat2);
        if (minY > maxY)
        {
            int temp = minY;
            minY = maxY;
            maxY = temp;
        }
        return new Box_alt(minX, maxX - minX, minY, maxY);
    }

    public static Box_alt ofXYXY(int x1, int y1, int x2, int y2)
    {
        return new Box_alt(x1, x2-x1, y1, y2);
    }

    public static Box_alt of(Bounds b)
    {
        int minX = b.minX();
        return new Box_alt(minX, b.maxX() - minX, b.minY(), b.maxY());
    }

    public static Box_alt ofWorld()
    {
        return new Box_alt(Integer.MIN_VALUE,-1,Integer.MIN_VALUE,Integer.MAX_VALUE);
    }
}