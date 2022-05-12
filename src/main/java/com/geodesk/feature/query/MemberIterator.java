package com.geodesk.feature.query;

import com.clarisma.common.util.Bytes;
import com.geodesk.feature.Feature;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.store.FeatureConstants;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredFeature;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class MemberIterator implements Iterator<Feature>
{
    private final FeatureStore store;
    private final ByteBuffer buf;
    private final Matcher filter;
    private int pCurrent;
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

    public MemberIterator(FeatureStore store, ByteBuffer buf, int pTable, Matcher filter)
    {
        this.store = store;
        this.buf = buf;
        pCurrent = pTable;
        this.filter = filter;
        fetchNextFeature();
    }

    private void roleChanged()
    {
        // do nothing
    }

    // TODO: could get rid of `member` by using the last-flag as a mask on pCurrent?
    private int fetchNext()
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
                // TODO: wide tip delta
                pForeignTile = 0;
                int tipDelta = buf.getShort(p);
                tipDelta >>= 1;     // signed
                tip += tipDelta;
                p += 2;
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
            roleChanged();
        }
        return p;
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
                if (pForeignTile == 0)
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
            if(filter.accept(featureBuf, pFeature))
            {
                StoredFeature f = store.getFeature(featureBuf, pFeature);
                f.setRole(role == -1 ? roleString : store.stringFromCode(role));
                memberFeature = (Feature) f;
                pCurrent = pNext;
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
