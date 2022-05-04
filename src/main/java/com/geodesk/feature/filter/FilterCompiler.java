package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FilterCompiler extends ClassLoader
{
    private final FilterParser parser;
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
        parser = new FilterParser(stringsToCodes, keysToCategories);
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

    private Filter createFilter(Selector selectors)
    {
        FilterCoder coder = new FilterCoder(valueNo);
        classCount++;
        String className = "Filter_" + classCount;
        byte[] code = coder.createFilterClass(className, selectors);
        Class<?> matcherClass = defineClass(className, code, 0, code.length);
        Filter filter;
        try
        {
            Constructor<?> constructor = matcherClass.getDeclaredConstructor(
                String.class.arrayType());
            // Can't pass globalStrings directly, since it is an array
            return (Filter)constructor.newInstance(new Object[]{ codesToStrings });
        }
        catch (NoSuchMethodException | SecurityException | InstantiationException |
               IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
            throw new QueryException("Filter compilation failed.", ex);
        }
    }

    private FilterSet createFilters(String query)
    {
        parser.parse(query);
        Selector selectors = parser.query();

        Selector sel = selectors;
        int commonType=0;
        while(sel != null)
        {
            int type = sel.matchTypes();
            if (commonType == 0)
            {
                commonType = type;
            }
            else
            {
                if (type != commonType) return createPolyformFilters(selectors);
            }
            sel = sel.next();
        }

        Filter filter = createFilter(selectors);
        return new FilterSet(
            (commonType & TypeBits.NODES) != 0 ? filter : null,
            (commonType & TypeBits.NONAREA_WAYS) != 0 ? filter : null,
            (commonType & TypeBits.AREAS) != 0 ? filter : null,
            (commonType & TypeBits.NONAREA_RELATIONS) != 0 ? filter : null);
    }

    /**
     * Extracts a chain of selectors that match the given type from
     * a source chain. The source chain must have a "dummy" head element.
     * If a Selector that matches the given type targets multiple types,
     * a shallow copy is created.
     *
     * @param selectors     a chain of Selectors with a dummy head
     * @param type          a bit field of types
     * @return  a chain of Selectors that target the given type
     */
    private Selector extractSelectors(Selector selectors, int type)
    {
        Selector firstExtracted = null;
        Selector lastExtracted = null;
        Selector prev = selectors;
        Selector next = prev.next();
        for(;;)
        {
            Selector sel = next;
            if(sel == null) break;
            next = sel.next();
            int selectorTypes = sel.matchTypes();
            if((selectorTypes & type) == type)
            {
                if(selectorTypes == type)
                {
                    // If the Selector matches only the selected type,
                    // take it from the chain
                    prev.setNext(next);
                    sel.setNext(null);
                }
                else
                {
                    // Otherwise, split off a copy
                    sel = sel.split(type);
                }
                if(firstExtracted == null)
                {
                    firstExtracted = sel;
                }
                else
                {
                    lastExtracted.setNext(sel);
                }
                lastExtracted = sel;
            }
        }
        return firstExtracted;
    }

    private FilterSet createPolyformFilters(Selector selectors)
    {
        // Create a "dummy" head to simplify the removal of elements
        // from the linked list of Selectors
        Selector head = new Selector(0);
        head.setNext(selectors);

        Selector selNodes     = extractSelectors(head, TypeBits.NODES);
        Selector selWays      = extractSelectors(head, TypeBits.NONAREA_WAYS);
        Selector selAreas     = extractSelectors(head, TypeBits.AREAS);
        Selector selRelations = extractSelectors(head, TypeBits.NONAREA_RELATIONS);
        assert head.next() == null: "All selectors must be extracted";

        return new FilterSet(
            selNodes     != null ? createFilter(selNodes)     : null,
            selWays      != null ? createFilter(selWays)      : null,
            selAreas     != null ? createFilter(selAreas)     : null,
            selRelations != null ? createFilter(selRelations) : null);
    }
}
