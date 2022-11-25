/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import java.util.concurrent.ForkJoinTask;

// TODO: do we need this base class?
public abstract class QueryTask extends ForkJoinTask<QueryResults>
{
    protected final Query query;
    protected QueryResults results;

    QueryTask(Query query)
    {
        this.query = query;
    }

    @Override public QueryResults getRawResult()
    {
        return results;
    }

    @Override protected void setRawResult(QueryResults value)
    {
        results = value;
    }
}
