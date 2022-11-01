/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import com.clarisma.common.util.Log;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class MatcherCompiler extends ClassLoader
{
    private final MatcherParser parser;
    private final String[] codesToStrings;
    private final int valueNo;
    private final Map<String, MatcherSet> matcherSets = new HashMap<>();
    private int classCount;

    // TODO: take FeatureStore as argument
    public MatcherCompiler(ObjectIntMap<String> stringsToCodes, String[] codesToStrings,
        IntIntMap keysToCategories)
    {
        this.codesToStrings = codesToStrings;
        valueNo = stringsToCodes.get("no");
        if(valueNo == 0) throw new QueryException("String table must include \"no\"");
        parser = new MatcherParser(stringsToCodes, keysToCategories);
    }

    public MatcherSet getMatchers(String query)
    {
        MatcherSet matchers = matcherSets.get(query);
        if(matchers == null)
        {
            matchers = createMatchers(query);
            matcherSets.put(query, matchers);
        }
        return matchers;
    }

    private Matcher createMatcher(Selector selectors)
    {
        MatcherCoder coder = new MatcherCoder(valueNo);
        classCount++;
        String className = "Filter_" + classCount;
        byte[] code = coder.createMatcherClass(className, selectors);
        Class<?> matcherClass = defineClass(className, code, 0, code.length);
        try
        {
            Constructor<?> constructor = matcherClass.getDeclaredConstructor(
                String.class.arrayType());
            // Can't pass globalStrings directly, since it is an array
            // TODO: pass FeatureStore, int types, resources
            return (Matcher)constructor.newInstance(new Object[]{ codesToStrings });
        }
        catch (NoSuchMethodException | SecurityException | InstantiationException |
               IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
            throw new QueryException("Matcher compilation failed.", ex);
        }
    }

    private MatcherSet createMatchers(String query)
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
                if (type != commonType)
                {
                    do
                    {
                        commonType |= sel.matchTypes();
                        sel = sel.next();
                    }
                    while (sel != null);
                    return createPolyformMatchers(commonType, selectors);
                }
            }
            sel = sel.next();
        }

        Matcher matcher = createMatcher(selectors);
        return new MatcherSet(commonType,
            (commonType & TypeBits.NODES) != 0 ? matcher : null,
            (commonType & TypeBits.NONAREA_WAYS) != 0 ? matcher : null,
            (commonType & TypeBits.AREAS) != 0 ? matcher : null,
            (commonType & TypeBits.NONAREA_RELATIONS) != 0 ? matcher : null,
            matcher);    // TODO: proper member filter
            // TODO: can use simple constructor once we've fixed the
            //  TileQueryTask
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
                Selector extracted;
                if(selectorTypes == type)
                {
                    // If the Selector matches only the selected type,
                    // take it from the chain
                    prev.setNext(next);
                    sel.setNext(null);
                    extracted = sel;
                    // Log.debug("took %s", sel);
                }
                else
                {
                    // Otherwise, split off a copy
                    extracted = sel.split(type);
                    // Log.debug("made a copy of %s", sel);
                }
                if(firstExtracted == null)
                {
                    firstExtracted = extracted;
                }
                else
                {
                    lastExtracted.setNext(extracted);
                }
                lastExtracted = extracted;
            }
            prev = sel;
        }
        return firstExtracted;
    }

    private MatcherSet createPolyformMatchers(int types, Selector selectors)
    {
        // Create a "dummy" head to simplify the removal of elements
        // from the linked list of Selectors
        Selector head = new Selector(0);
        head.setNext(selectors);

        // Log.debug("extracting selectors...");
        Selector selNodes     = extractSelectors(head, TypeBits.NODES);
        assert selNodes == null || selNodes.matchTypes() == TypeBits.NODES;
        Selector selWays      = extractSelectors(head, TypeBits.NONAREA_WAYS);
        assert selWays == null || selWays.matchTypes() == TypeBits.NONAREA_WAYS;
        Selector selAreas     = extractSelectors(head, TypeBits.AREAS);
        assert selAreas == null || selAreas.matchTypes() == TypeBits.AREAS;
        Selector selRelations = extractSelectors(head, TypeBits.NONAREA_RELATIONS);
        assert selRelations == null || selRelations.matchTypes() == TypeBits.NONAREA_RELATIONS;

        /*
        int testTypes = 0;
        if(selNodes != null) testTypes |= TypeBits.NODES;
        if(selWays != null) testTypes |= TypeBits.NONAREA_WAYS;
        if(selAreas != null) testTypes |= TypeBits.AREAS;
        if(selRelations != null) testTypes |= TypeBits.NONAREA_RELATIONS;
        assert types == testTypes;
         */

        // It's possible to leave the original selectors behind; it's easier
        // to make a type-specific copy -- so no assert
        // assert head.next() == null: "All selectors must be extracted";

        return new MatcherSet(types,
            selNodes     != null ? createMatcher(selNodes)     : null,
            selWays      != null ? createMatcher(selWays)      : null,
            selAreas     != null ? createMatcher(selAreas)     : null,
            selRelations != null ? createMatcher(selRelations) : null,
            null);      // TODO: member filter
    }
}
