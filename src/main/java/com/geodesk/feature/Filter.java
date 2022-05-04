package com.geodesk.feature;

import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public interface Filter
{
    boolean accept(ByteBuffer buf, int pos);
    boolean acceptTyped(int types, ByteBuffer buf, int pos);
    // boolean accept(ByteBuffer buf, int pos, int roleGroup);
    boolean acceptIndex(int keys);
    boolean acceptGeometry(Geometry geom);
    // int acceptRole(int roleCode, String roleString);

    /*
    public static final int NODES = 1;
    public static final int WAYS = 2;
    public static final int AREAS = 4;
    public static final int RELATIONS = 8;
    */

    public static final Filter ALL = new Filter()
    {
        @Override public boolean accept(ByteBuffer buf, int pos)
        {
            return true;
        }

        @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
        {
            return true;
        }

        /*
        @Override public boolean accept(ByteBuffer buf, int pos, int roleGroup)
        {
            return true;
        }
        @Override public int acceptRole(int roleCode, String roleString)
        {
            return 1;
        }
         */
        @Override public boolean acceptIndex(int keys)
        {
            return true;
        }

        @Override public boolean acceptGeometry(Geometry geom)
        {
            return true;
        }
    };
}
