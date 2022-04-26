package com.geodesk.feature.query;

import com.geodesk.feature.filter.Filter;
import com.geodesk.feature.filter.FilterSet;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FilterCompiler extends ClassLoader
{
    private final QueryParser parser;
    private final String[] codesToStrings;
    private final int valueNo;
    private final Map<String, FilterSet> filterSets = new HashMap<>();
    private int classCount;

    public FilterCompiler(ObjectIntMap<String> stringsToCodes, String[] codesToStrings,
        IntIntMap keysToCategories)
    {
        this.codesToStrings = codesToStrings;
        valueNo = stringsToCodes.get("no");
        if(valueNo == 0) throw new QueryException("String table must include \"no\"");
        parser = new QueryParser(stringsToCodes, keysToCategories);
    }

    public FilterSet getFilters(String query)
    {
        FilterSet filters = filterSets.get(query);
        if(filters == null)
        {
            filters = createFilters(query);
            filterSets.put(query, filters);
        }
        return filters;
    }

    private FilterSet createFilters(String query)
    {
        QueryCoder coder = new QueryCoder(valueNo);

        classCount++;
        String className = "Filter_" + classCount;
        Filter nodes = null;
        Filter ways = null;
        Filter areas = null;
        Filter relations = null;

        parser.parse(query);
        Selector sel = parser.query();
        while(sel != null)
        {
            int type = sel.matchTypes();
            Selector c = sel;
            Selector next;
            for (; ; )
            {
                next = c.next();
                if (next == null || next.matchTypes() != type) break;
                c = next;
            }

            byte[] code = coder.createFilterClass(className, sel);
            Class<?> matcherClass = defineClass(className, code, 0, code.length);
            Filter filter;
            try
            {
				Constructor<?> constructor = matcherClass.getDeclaredConstructor(
                    String.class.arrayType());
                // Can't pass globalStrings directly, since it is an array
                filter = (Filter)constructor.newInstance(new Object[]{ codesToStrings });
            }
            catch (NoSuchMethodException | SecurityException | InstantiationException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
            {
                throw new QueryException("Filter compilation failed.", ex);
            }

            // TODO: assert that there is only one Matcher for each type
            // .e. assert query.matchNodes==null;
            if((type & Selector.MATCH_NODES) != 0) nodes = filter;
            if((type & Selector.MATCH_WAYS) != 0) ways = filter;
            if((type & Selector.MATCH_AREAS) != 0) areas = filter;
            if((type & Selector.MATCH_RELATIONS) != 0) relations = filter;

            sel = next;
        }
        return new FilterSet(nodes,ways,areas,relations);
    }

}
