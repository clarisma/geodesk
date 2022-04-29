package com.geodesk.feature;

import com.geodesk.core.Box;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedPolygon;

import java.util.HashSet;
import java.util.Set;

import static java.lang.System.out;


public class SpatialFilterTest
{
    @Test public void testSpatial()
    {
        long start = System.currentTimeMillis();
        FeatureLibrary world = new FeatureLibrary("c:\\geodesk\\tests\\de.gol");
        Feature bavaria = world
            .features("a[boundary=administrative][admin_level=4][name:en=Bavaria]")
            .in(Box.atLonLat(12.0231, 48.3310))
            .first();
        Geometry bavariaPoly = bavaria.toGeometry();
        PreparedPolygon bavariaPrepared = new PreparedPolygon((Polygonal)bavariaPoly);

        long startQuery = System.currentTimeMillis();

        /*
        Features<?> places = world
            .features("n[place=city], n[place=town][population > 25000]");
         */
        Features<?> places = world
            .features("a[leisure=pitch][sport=soccer]");
        int count=0;

        Set<Feature> found = new HashSet<>();

        for(Feature place: places.in(Box.of(bavariaPoly)))
        {
            // out.println(place.stringValue("name"));
            // count++;
            /*
            if(bavariaPoly.contains(place.toGeometry()))
            {
            //    out.println(place.stringValue("name"));
                count++;
            }
             */
            if(bavariaPrepared.contains(place.toGeometry()))
            {
                // out.println(place.stringValue("name"));
                found.add(place);
                count++;
            }
        }

        for(Feature place: places.in(Box.of(bavariaPoly)))
        {
            // out.println(place.stringValue("name"));
            // count++;
            /*
            if(bavariaPoly.contains(place.toGeometry()))
            {
            //    out.println(place.stringValue("name"));
                count++;
            }
             */
            if(bavariaPrepared.intersects(place.toGeometry()))
            {
                if(!found.contains(place))
                {
                    out.format("%s intersects, but not contained in\n", place);
                }
            }
        }
        long end = System.currentTimeMillis();
        out.format("Found %d features in %d ms (Total runtime %d ms)\n", count,
            end-startQuery, end-start);
    }

    @Test public void testSpatialBuildings()
    {
        long start = System.currentTimeMillis();
        FeatureLibrary world = new FeatureLibrary("c:\\geodesk\\tests\\de.gol");
        Feature bavaria = world
            .features("a[boundary=administrative][admin_level=4][name:en=Bavaria]")
            .in(Box.atLonLat(12.0231, 48.3310))
            .first();
        Geometry bavariaPoly = bavaria.toGeometry();
        PreparedPolygon bavariaPrepared = new PreparedPolygon((Polygonal)bavariaPoly);

        for(int i=0; i<10; i++)
        {
            long startQuery = System.currentTimeMillis();
            Features<?> places = world.features("a[building]");
            long count = 0; // places.in(Box.of(bavariaPoly)).count();

            for(Feature place: places.in(Box.of(bavariaPoly)))
            {
                Geometry candidateGeom = place.toGeometry();
                if(candidateGeom != null && bavariaPrepared.contains(candidateGeom))
                {
                    count++;
                }
            }

            long end = System.currentTimeMillis();
            out.format("Found %d features in %d ms (Total runtime %d ms)\n", count,
                end - startQuery, end - start);
        }
    }

}
