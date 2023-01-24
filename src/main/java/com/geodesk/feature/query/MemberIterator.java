/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.clarisma.common.util.Bytes;
import com.clarisma.common.util.Log;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.store.FeatureConstants;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredFeature;

import java.nio.ByteBuffer;
import java.util.Iterator;

// TODO: filter
// TODO: fix lazy tile loading
//  pForeignTile needs two special values:
//    a) no attempt has yet been made to fetch the foreign tile
//    b) tried to fetch foreign tile, but it was missing

public class MemberIterator implements Iterator<Feature>
{
    private final FeatureStore store;
    private final ByteBuffer buf;
    private final int types;
    private final Matcher matcher;
    private final Filter filter;
    private int pCurrent;
    private Matcher currentMatcher;
    private int role;
    private String roleString;
    private int tip = FeatureConstants.START_TIP;
    private ByteBuffer foreignBuf;
    private int pForeignTile;
    private int member;
    private Feature memberFeature;

    // TODO: consolidate these flags?
    private static final int MF_LAST = 1;
    private static final int MF_FOREIGN = 2;
    private static final int MF_DIFFERENT_ROLE = 4;
    private static final int MF_DIFFERENT_TILE = 8;

    public MemberIterator(FeatureStore store, ByteBuffer buf, int pTable,
        int types, Matcher matcher, Filter filter)
    {
        this.store = store;
        this.buf = buf;
        pCurrent = pTable;
        this.types = types;
        this.matcher = matcher;
        this.filter = filter;
        currentMatcher = matcher.acceptRole(0, null);
            // TODO: skip call to acceptRole if first member has DIFFERENT_ROLE flag set
        fetchNextFeature();
    }

    // TODO: should this return `member` and set `pCurrent` itself?
    // TODO: could get rid of `member` by using the last-flag as a mask on pCurrent?

    /**
     * Identifies the next member whose role is accepted by the Matcher.
     *
     * Before this call:
     *
     * - `pCurrent` must be set to the next potential member
     * - `member` must contain the last-flag of the previous member
     *   (used to determine if we've reached the end of the table)
     *
     * After this call:
     *  - `member` contains the member entry (flags & pointer) of the
     *    current member
     *  - `pCurrent` contains the address where the data of `member` is stored
     *
     *  If there are no more members:
     *  - `member` is set to 0
     *  - `pCurrent` points to the byte after the table (and hence is invalid)
     *
     * @return  pointer to the member after the current, or 0 if we've
     *          reached the end of the table
     */
    private int fetchNext()
    {
        for(;;)
        {
            int p = pCurrent;
            if ((member & MF_LAST) != 0)
            {
                member = 0;
                return 0;
            }
            member = buf.getInt(p);
            p += 4;
            if ((member & MF_FOREIGN) != 0)
            {
                if ((member & MF_DIFFERENT_TILE) != 0)
                {
                    // TODO: test wide tip delta
                    pForeignTile = 0;       // TODO: set to other value, 0 if valid tile start
                    int tipDelta = buf.getShort(p);
                    if ((tipDelta & 1) != 0)
                    {
                        // wide TIP delta
                        tipDelta = buf.getInt(p);
                        p += 2;
                    }
                    tipDelta >>= 1;     // signed
                    p += 2;
                    tip += tipDelta;
                }
            }
            if ((member & MF_DIFFERENT_ROLE) != 0)
            {
                int rawRole = buf.getChar(p);
                if ((rawRole & 1) != 0)
                {
                    // common role
                    role = rawRole >>> 1;   // unsigned
                    roleString = null;
                    p += 2;
                }
                else
                {
                    rawRole = buf.getInt(p);
                    role = -1;
                    roleString = Bytes.readString(buf, p + (rawRole >> 1)); // signed
                    p += 4;
                }
                currentMatcher = matcher.acceptRole(role, roleString);
            }
            if(currentMatcher != null) return p;
            pCurrent = p;
        }
    }

    private void fetchNextFeature()
    {
        for(;;)
        {
            int pNext = fetchNext();
            if (pNext == 0)
            {
                memberFeature = null;
                return;
            }
            ByteBuffer featureBuf;
            int pFeature;
            if ((member & MF_FOREIGN) != 0)
            {
                if (pForeignTile == 0)  // TODO: Tile could start at segment start!
                {
                    int tilePage = store.fetchTile(tip);
                    foreignBuf = store.bufferOfPage(tilePage);
                    pForeignTile = store.offsetOfPage(tilePage);
                }
                featureBuf = foreignBuf;
                pFeature = pForeignTile + ((member >>> 4) << 2);
            }
            else
            {
                featureBuf = buf;
                pFeature = (pCurrent & 0xffff_fffc) + ((member >> 3) << 2);
            }
            pCurrent = pNext;
            if (currentMatcher.acceptTyped(types, featureBuf, pFeature))
            {
                StoredFeature f = store.getFeature(featureBuf, pFeature);
                // TODO: allow any negative instead of -1?
                f.setRole(role == -1 ? roleString : store.stringFromCode(role));
                memberFeature = f;
                // pCurrent = pNext;
                return;
            }
        }
    }

    @Override public boolean hasNext()
    {
        return memberFeature != null;
    }

    @Override public Feature next()
    {
        Feature next = memberFeature;
        fetchNextFeature();
        return next;
    }
}
