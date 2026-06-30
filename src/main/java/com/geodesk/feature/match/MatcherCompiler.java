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
    private final Map<String, Matcher> matchers = new HashMap<>();
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

    public Matcher getMatcher(String query)
    {
        Matcher matcher = matchers.get(query);
        if(matcher == null)
        {
            matcher = createMatcher(query);
            matchers.put(query, matcher);
        }
        return matcher;
    }

    private Matcher createMatcher(Selector selectors)
    {
        MatcherCoder coder = new MatcherCoder(valueNo);
        classCount++;
        String className = "Matcher_" + classCount;
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

    private Matcher createMatcher(String query)
    {
        parser.parse(query);
        Selector selectors = parser.query();
        return createMatcher(selectors);
    }
}
