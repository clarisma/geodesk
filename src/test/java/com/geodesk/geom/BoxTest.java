package com.geodesk.geom;

import com.geodesk.core.Box;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.Integer.*;

public class BoxTest
{
    public static final Logger log = LogManager.getLogger();

    private void testContain(Box box, int x, int y, boolean shouldContain)
    {
        assertEquals(shouldContain, box.contains(x,y));
    }

    private void testIntersect(Box a, Box b, boolean res)
    {
        assertEquals(res, a.intersects(b));
        assertEquals(res, b.intersects(a));
    }

    @Test public void test()
    {
        Box box = Box.ofWorld();
        log.debug("World: {}", box);
        box.buffer(10);
        log.debug("World + 10: {}", box);
        box.buffer(-10);
        log.debug("World - 10: {}", box);
        box.buffer(Integer.MIN_VALUE);
        log.debug("World - 10 - max: {}", box);

        box = new Box();
        log.debug("Should be empty: {}", box);
        box.expandToInclude(90, 100);
        log.debug("Should contain 90,100: {}", box);
        box.expandToInclude(-4000, -8000);
        log.debug("Added -4K,-8K: {}", box);
        box.buffer(200);
        log.debug(" + 200: {}", box);
        log.debug(" + 200: {}", box);
        testContain(box, 0,0, true);
        testContain(box, -7000,-3000, false);

        Box box2 = Box.ofWSEN(170, -40, -160, 30);
        testContain(box2, Integer.MIN_VALUE,-3000, true);
        testContain(box2, Integer.MAX_VALUE,-3000, true);
        testContain(box2, 0,0, false);


    }

    Box EMPTY = new Box();
    Box A = new Box(-800, 600, -100, 800);
    Box B = new Box(100, 500, 700, 800);
    Box C = new Box(-900, MIN_VALUE, -700, -200);
    Box D = new Box(300, -700, 800, -300);
    Box E = new Box(-300, 300, 200, 900);
    Box F = new Box(-700, 200, -200, 700);
    Box G = new Box(600, 300, MAX_VALUE, 600);
    Box H = new Box(-800, -300, 500, 300);

    @Test
    public void testIntersects()
    {
        testIntersect(A,B,false);
        testIntersect(A,C,false);
        testIntersect(A,D,false);
        testIntersect(B,C,false);
        testIntersect(B,D,false);
        testIntersect(C,D,false);

        testIntersect(E,A,true);
        testIntersect(E,B,true);
        testIntersect(E,C,false);
        testIntersect(E,D,false);

        testIntersect(F,A,true);
        testIntersect(F,B,false);
        testIntersect(F,C,false);
        testIntersect(F,D,false);
        testIntersect(F,E,true);

        testIntersect(G,A,false);
        testIntersect(G,B,true);
        testIntersect(G,C,false);
        testIntersect(G,D,false);
        testIntersect(G,E,false);
        testIntersect(G,F,false);

        testIntersect(H,A,false);
        testIntersect(H,B,false);
        testIntersect(H,C,true);
        testIntersect(H,D,true);
        testIntersect(H,E,true);
        testIntersect(H,F,true);
        testIntersect(H,G,false);

        /*
        testIntersect(EMPTY,A,false);
        testIntersect(EMPTY,B,false);
        testIntersect(EMPTY,C,false);
        testIntersect(EMPTY,D,false);
        testIntersect(EMPTY,E,false);
        testIntersect(EMPTY,F,false);
        testIntersect(EMPTY,G,false);
        testIntersect(EMPTY,H,false);
         */
    }

    @Test
    public void testArea()
    {
        assertEquals(140901, A.area());
        assertEquals(200901, D.area());
    }

    @Test
    public void testTranslate()
    {
        Box h = new Box(H);
        log.debug(h);
        h.translate(MIN_VALUE,0);
        log.debug(h);
        h.translate(MIN_VALUE,0);
        log.debug(h);
    }

}