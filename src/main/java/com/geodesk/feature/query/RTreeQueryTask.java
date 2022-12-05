/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.clarisma.common.util.Log;
import com.geodesk.feature.FeatureType;
import com.geodesk.feature.Filter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.StoredFeature;
import com.geodesk.feature.store.StoredNode;

import java.nio.ByteBuffer;

// TODO: make Nodes the base class, Ways/Relations the specialization?

public class RTreeQueryTask extends QueryTask
{
    protected final ByteBuffer buf;
    protected final int ppTree;
    protected final int bboxFlags;
    protected final Matcher matcher;
    protected final Filter filter;
    protected final RTreeQueryTask next;

    public RTreeQueryTask(TileQueryTask parent, int ppTree, Matcher matcher, RTreeQueryTask next)
    {
        super(parent.query);
        this.buf = parent.buf;
        this.ppTree = ppTree;
        this.bboxFlags = parent.bboxFlags;
        this.matcher = matcher;
        this.filter = parent.filter;
        this.next = next;
    }

    @Override protected boolean exec()
    {
        try
        {
            results = new QueryResults(buf);
            int ptr = buf.getInt(ppTree);
            if (ptr != 0)
            {
                if ((ptr & 2) != 0)
                {
                    searchLeaf(ppTree + (ptr & 0xffff_fffc));
                }
                else
                {
                    searchTrunk(ppTree + (ptr & 0xffff_fffc));
                }
            }
        }
        catch(Throwable ex)
        {
            query.setError(ex);
        }
        return true;
    }

    private void searchTrunk(int p)
    {
        // log.debug("Searching trunk SIB at {}", String.format("%08X", p));
        int minX = query.minX();
        int minY = query.minY();
        int maxX = query.maxX();
        int maxY = query.maxY();
        for (; ; )
        {
            int ptr = buf.getInt(p);
            int last = ptr & 1;

            if (!(buf.getInt(p + 4) > maxX ||
                buf.getInt(p + 8) > maxY ||
                buf.getInt(p + 12) < minX ||
                buf.getInt(p + 16) < minY))
            {
                if ((ptr & 2) != 0)
                {
                    searchLeaf(p + (ptr ^ 2 ^ last));
                }
                else
                {
                    searchTrunk(p + (ptr ^ last));
                }
            }
            else
            {
                // log.debug("SIB rejected");
            }
            if (last != 0) break;
            p += 20;
        }
    }

    protected void searchLeaf(int p)
    {
        // log.debug("Searching leaf SIB at {}", String.format("%08X", p));
        int minX = query.minX();
        int minY = query.minY();
        int maxX = query.maxX();
        int maxY = query.maxY();
        int acceptedTypes = query.types();

        for(;;)
        {
            int flags = buf.getInt(p + 16);
            /*
            if(StoredFeature.id(buf, p+16) == 128815778)
            {
                Log.debug("way/128815778");
            }
             */

            int multiTileFlags = flags & FeatureFlags.MULTITILE_FLAGS;
            for(;;)
            {
                // Check for acceptable type (way, relation, member, way-node, etc.)
                // (No need for AND with 0x1f, as int-shift only considers lower 5 bits)
                if(((1 << (flags >> 1)) & acceptedTypes) == 0) break;

                // TODO: we could use masks and save the coordinate comparison step!
                // we only have to make one check

                // TODO
                //  Careful: If both multi-tile flags are set for the feature
                //  (which means it lives in more than 2 tiles), we must always
                //  treat it as a potential dupe, even if bbox extends only into
                //  one neighbouring tile
                // But we can check (multiTileFlags & bboxFlags) == 0, if so,
                //  no checks needed at all

                int dupeFlag = 0;
                if(multiTileFlags != 0)
                {
                    if (multiTileFlags == FeatureFlags.MULTITILE_WEST)
                    {
                        // If the feature has a second copy in the tile
                        // to the west, and the query's bounding box
                        // extends into that tile, we skip the feature

                        // TODO: use masking instead
                        if ((bboxFlags & FeatureFlags.MULTITILE_WEST) != 0) break;
                    }
                    else if (multiTileFlags == FeatureFlags.MULTITILE_NORTH)
                    {
                        // If the feature has a second copy in the tile
                        // to the north, and the query's bounding box
                        // extends into that tile, we skip the feature

                        // TODO: use masking instead
                        if ((bboxFlags & FeatureFlags.MULTITILE_NORTH) != 0) break;
                    }
                    else
                    {
                        // If both flags are set, this means we'll have
                        // to add the feature to the deduplication set
                        // TODO: if query does not extend beyond the
                        // tile boundaries, we don't have to do this
                        dupeFlag = 0x8000_0000;
                    }
                }
                if (!(buf.getInt(p) > maxX ||
                    buf.getInt(p + 4) > maxY ||
                    buf.getInt(p + 8) < minX ||
                    buf.getInt(p + 12) < minY))
                {
                    // TODO: replace this branching code with arithmetic?
                    // Useful? https://stackoverflow.com/a/62852710

                    // log.debug("Feature bbox matched");
                    int pFeature = p + 16;
                    if (matcher.accept(buf, pFeature))
                    {
                        // TODO: We should return results as Features rather than pointers,
                        //  since we are creating a Feature anyway in order to apply a filter
                        if(filter == null || filter.accept(query.store().getFeature(buf, pFeature)))
                        {
                            results.add(pFeature | ((flags >>> 3) & 3) | dupeFlag);
                        }
                    }
                }
                else
                {
                    // log.debug("Feature rejected");
                }
                break;
            }
            if((flags & 1) != 0) break;
            p += 32;
        }
    }

    public static class Nodes extends RTreeQueryTask
    {
        public Nodes(TileQueryTask parent, int ppTree, Matcher matcher, RTreeQueryTask next)
        {
            super(parent, ppTree, matcher, next);
        }

        @Override protected void searchLeaf(int p)
        {
            int minX = query.minX();
            int minY = query.minY();
            int maxX = query.maxX();
            int maxY = query.maxY();
            for(;;)
            {
                int flags = buf.getInt(p + 8);
                int x = buf.getInt(p);
                int y = buf.getInt(p+4);
                if(!(x > maxX || y > maxY || x < minX || y < minY))
                {
                    int pFeature = p+8;
                    if(matcher.accept(buf, pFeature))
                    {
                        // TODO: We should return results as Features rather than pointers,
                        //  since we are creating a Feature anyway in order to apply a filter
                        if(filter == null || filter.accept(
                            new StoredNode(query.store(), buf, pFeature)))
                        {
                            results.add(pFeature);
                        }
                    }
                }
                if((flags & 1) != 0) break;
                p += 20 + (flags & 4);
                    // If Node is member of relation (flag bit 2), add
                    // extra 4 bytes for the relation table pointer
            }
        }
    }
}
