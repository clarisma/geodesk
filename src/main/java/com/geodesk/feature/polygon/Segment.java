/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.polygon;

import com.geodesk.feature.Way;

public class Segment
{
    final static int UNASSIGNED = 0;
    final static int TENTATIVE = 1;
    final static int ASSIGNED = 2;
    final static int DANGLING = 3;

    final int number;
    final Way way;
    final int[] coords;
    Segment next;
    boolean backward;
    byte status;

    Segment(int number, Way way, Segment next)
    {
        this.number = number;
        this.way = way;
        coords = way.toXY();
        this.next = next;
    }
}
