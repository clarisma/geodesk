/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.core.Box;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

public class BoxCoordinateSequence extends Box implements CoordinateSequence
{
    public BoxCoordinateSequence(int minX, int minY, int maxX, int maxY)
    {
        super(minX, minY, maxX, maxY);
    }

    public BoxCoordinateSequence(Bounds b)
    {
        super(b.minX(), b.minY(), b.maxX(), b.maxY());
    }

    public final int x(int n)
    {
        return (((n+1) & 2) != 0) ? minX() : maxX();
    }

    public final int y(int n)
    {
        return ((n & 2) != 0) ? minY() : maxY();
    }

    @Override public Envelope expandEnvelope(Envelope env)
    {
        env.expandToInclude(minX(), minY());
        env.expandToInclude(maxX(), maxY());
        return env;
    }

    @Override public Coordinate getCoordinate(int n)
    {
        return new Coordinate(x(n), y(n));
    }

    @Override public void getCoordinate(int n, Coordinate c)
    {
        c.setOrdinate(0, x(n));
        c.setOrdinate(1, y(n));
    }

    @Override public Coordinate getCoordinateCopy(int n)
    {
        return getCoordinate(n);
    }

    @Override public int getDimension()
    {
        return 2;
    }

    @Override public double getOrdinate(int n, int dimension)
    {
        return dimension == 0 ? x(n) : y(n);
    }

    @Override public double getX(int n)
    {
        return x(n);
    }

    @Override public double getY(int n)
    {
        return y(n);
    }

    @Override public void setOrdinate(int arg0, int arg1, double arg2)
    {
        throw new RuntimeException("Coordinates are immutable.");
    }

    @Override public int size()
    {
        return 5;
    }

    @Override public Coordinate[] toCoordinateArray()
    {
        Coordinate[] c = new Coordinate[size()];
        for(int i=0; i<size(); i++)
        {
            c[i] = getCoordinate(i);
        }
        return c;
    }

    // TODO: check
    public CoordinateSequence clone()
    {
        return new CoordinateArraySequence(toCoordinateArray());
    }

    // TODO: check
    public CoordinateSequence copy()
    {
        return new CoordinateArraySequence(toCoordinateArray());
    }
}
