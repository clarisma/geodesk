/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.polygon;

public class RingAssigner
{
    private static void assignRing(Ring[] outerRings, Ring inner)
    {
        Ring tentativeOuter = null;
        for(int i=outerRings.length-1; i>0; i--)
        {
            Ring tryOuter = outerRings[i];
            if(tryOuter.bbox.contains(inner.bbox))
            {
                if(tentativeOuter != null && tentativeOuter.contains(inner))
                {
                    tentativeOuter.addInner(inner);
                    return;
                }
                tentativeOuter = tryOuter;
            }
        }
        if(tentativeOuter != null && tentativeOuter.contains(inner))
        {
            tentativeOuter.addInner(inner);
            return;
        }
        outerRings[0].addInner(inner);
    }

    public static Ring[] assignRings(Ring firstOuter, Ring firstInner)
    {
        // count rings and determine the largest ring

        int outerCount = 1;
        int maxCoordinates = 0;
        Ring biggestOuter = null;
        Ring outer = firstOuter;
        for(;;)
        {
            if(outer.coordinateCount > maxCoordinates)
            {
                maxCoordinates = outer.coordinateCount;
                biggestOuter = outer;
            }
            outer = outer.next;
            if(outer == null) break;
            outerCount++;
        }

        // Calculate bboxes of all rings, except for the largest

        Ring[] outerRings = new Ring[outerCount];
        outer = firstOuter;
        for(int i=outerCount-1;;)
        {
            if(outer != biggestOuter)
            {
                outer.calculateBounds();
                outerRings[i] = outer;
                i--;
            }
            outer = outer.next;
            if(outer == null) break;
        }
        outerRings[0] = biggestOuter;

        Ring inner = firstInner;
        for(;;)
        {
            inner.calculateBounds();
            Ring next = inner.next;
                // assignRing may change next because it re-chains the rings
            assignRing(outerRings, inner);
            if(next == null) break;
            inner = next;
        }
        return outerRings;
    }
}
