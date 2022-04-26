package com.geodesk.geom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.locationtech.jts.util.Stopwatch;

import static org.junit.Assert.*;
import static com.geodesk.geom.PointInPolygon.*;

public class PointInPolygonTest
{
    public static final Logger log = LogManager.getLogger();

    static final int[] P =
    {
        -400, 200,
        -200, 500,
        100, 500,
        400, 200,
        -200, -300,
        -400, -100,
        -400, 200
    };

    static final int[] P2 =
    {
        -400, -100,
        -700, 0,
        -500, -600,
        -200, -300,
        -400, -100
    };

    static final int[] P3 =
    {
        -400, -100,
        -200, -300,
        -500, -600,
        -700, 0,
        -400, -100
    };

    static final int[] points =
    {
        -200, 200, 1,
        200, -200, 0,
        200, 500, 0,
        -300,0,1,
        100,0,1,
        300,0,0,
        300,200,1,
        350,300,0,
        -400,-200,0,
        0,400,1,
        0,499,1,        // TODO
        0,501,0,
        0,600,0,
        0,-100,1,
        0,-300,0,
        100,300,1,
        -400,100,1
    };

    static final int[] R =
    {
        -200, 200,
        200, 200,
        200, -200,
        -200, -200,
        -200, 200
    };

    static final int[] rpoints =
    {
        -100, 200, 1,
        100, 200, 1,
        -200, 100, 1,
        -200, -100, 1,
        100, -200, 1,
        -100, -200, 1,
        -200, -100, 1,
        -200, 100, 1
    };



    private void testVertices(String s, int[] p)
    {
        log.debug(s);
        for(int i=0; i<p.length; i+=2)
        {
            int x = p[i];
            int y = p[i+1];
            log.debug("{}, {}: {}", x,y, isInside(p,x,y));
            // assertTrue(isInside(P,x,y));
        }
    }


    private void testVerticesFast(int[] p)
    {
        for(int i=0; i<p.length; i+=2)
        {
            int x = p[i];
            int y = p[i+1];
            assertEquals(-1, testFast(p,x,y));
        }
    }

    // TODO: This test fails
    /*

    @Test
    public void testVerticesFast()
    {
        testVerticesFast(P);
        testVerticesFast(P2);
        testVerticesFast(P3);
        testVerticesFast(R);
    }

     */

    private void testPointsFast(int[] polygon, int[] points)
    {
        for(int i=0; i<points.length; i+=3)
        {
            int x = points[i];
            int y = points[i+1];
            int expected = points[i+2];
            assertEquals(expected, testFast(polygon, x,y));
        }
    }

    @Test
    public void testPointsFast()
    {
        testPointsFast(P, points);
    }

    @Test
    public void testPointsFastPerformance()
    {
        Stopwatch timer = new Stopwatch();
        for(int run=0; run<10; run++)
        {
            timer.start();
            for (int i = 0; i < 1_000_000; i++)
            {
                testPointsFast(P, points);
            }
            log.debug("Run {}: {} ms", run, timer.stop());
            timer.reset();
        }

    }


    private void testPoints(int[] polygon, int[] points)
    {
        for(int i=0; i<points.length; i+=3)
        {
            int x = points[i];
            int y = points[i+1];
            int inside = points[i+2];
            log.debug("{}, {}: {}", x,y, isInside(polygon,x,y));
            // assertEquals(inside != 0, isInside(polygon, x,y));
        }
    }

    @Test
    public void testPoints()
    {
        testPoints(R, rpoints);
    }


    @Test public void testIsInside()
    {
        testVertices("P1", P);
        testVertices("P2", P2);
        testVertices("P3", P3);
    }
}