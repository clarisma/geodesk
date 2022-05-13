package com.geodesk.feature.store;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

/**
 * A {@link CoordinateSequence} that provides integer-based coordinates
 * in a compact format.
 */
// TODO: is this applicable to geometries other than Way features?
public class WayCoordinateSequence implements CoordinateSequence
{
    private final int[] coordinates;	// pairs of x/y

    public WayCoordinateSequence(int[] coords)
    {
        this.coordinates = coords;
    }

    public final int x(int n)
    {
        return coordinates[n*2];
    }

    public final int y(int n)
    {
        return coordinates[n*2+1];
    }


    @Override public Envelope expandEnvelope(Envelope env)
    {
        for(int i=0; i<size(); i++)
        {
            env.expandToInclude(x(i), y(i));
        }
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

    @Override
    public double getOrdinate(int n, int dimension)
    {
        return coordinates[n * 2 + dimension];
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
        throw new RuntimeException("Coordinates of a Way are immutable.");
    }

    @Override public int size()
    {
        return coordinates.length / 2;
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
