package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;
import com.geodesk.feature.store.StoredFeature;

import java.nio.ByteBuffer;

//public class PolygonLineworkFilter implements Filter
//{
//    @Override public boolean accept(ByteBuffer buf, int pos)
//    {
//        return false;
//    }
//
//    @Override public boolean accept(ByteBuffer buf, int pos, int roleGroup)
//    {
//        return StoredFeature.type(buf, pos) == 1;
//    }
//
//    @Override public boolean acceptIndex(int keys)
//    {
//        return true;
//    }
//
//    @Override public int acceptRole(int roleCode, String roleString)
//    {
//        // TODO: only allow outer or inner
//        return 1;
//    }
//}
