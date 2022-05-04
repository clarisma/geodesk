package com.geodesk.feature.query;

import java.util.concurrent.ForkJoinTask;

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
