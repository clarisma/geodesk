/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.parser;

import com.clarisma.common.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A parser that reads comma-separated `key=value` tags. `value`
 * can be a quoted string, an identifier (treated as a string),
 * a number, `true` or `false`.
 *
 */
public class TagsParser extends Parser
{
	public static final String COMMA = ",";
	public static final String EQUALS = "=";
	public static final String TRUE = "true";
	public static final String FALSE = "false";

	protected static final Object TOKENS[] =
	{
		COMMA, EQUALS, TRUE, FALSE
	};
	
	// TODO: fix!!!
	public final static Pattern KEY_PATTERN =
		Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_\\-:\\.]*");
		

	public TagsParser()
	{
		for(Object tok : TOKENS) 
		{
			addToken(tok.toString(), tok);
		}
		setIdentifierPattern(KEY_PATTERN);
	}
	
	public String key()
	{
		if(tokenType == STRING)
		{
			String key = unquotedStringValue();
			nextToken();
			return key;
		}
		if(tokenType == IDENTIFIER)
		{
			String key = stringValue();
			nextToken();
			return key;
		}
		error("Expected <key>, but got %s", tokenType);
		return null;
	}
	
	public Object value()
	{
		if(tokenType == STRING)
		{
			Object value = unquotedStringValue();
			nextToken();
			return value;
		}
		if(tokenType == IDENTIFIER)
		{
			Object value = stringValue();
			nextToken();
			return value;
		}
		if(tokenType == NUMBER)
		{
			Object value;
			if(stringValue().indexOf('.') >= 0)
			{
				value = doubleValue();
			}
			else
			{
				value = longValue();	
			}
			nextToken();
			return value;
		}
		if(tokenType==TRUE)
		{
			nextToken();
			return Boolean.TRUE;
		}
		if(tokenType==FALSE)
		{
			nextToken();
			return Boolean.FALSE;
		}
		error("Expected <value>, but got %s", tokenType);
		return null;
	}
	
	public Map<String,Object> tags()
	{
		Map<String, Object> map = new HashMap<>();
		
		for(;;)
		{
			String key = key();
			expect(EQUALS);
			nextToken();
			map.put(key, value());
			if(!hasMore()) break;
			acceptAndConsume(COMMA);
				// TODO: should accept only comma or newline
			// expect(COMMA);
			// nextToken();
		}
		return map;
	}
	
	public List<String> tagsAsList()
	{
		List<String> list = new ArrayList<>();
		
		for(;;)
		{
			list.add(key());
			expect(EQUALS);
			nextToken();
			list.add(value().toString());
			if(!hasMore()) break;
			expect(COMMA);
			nextToken();
		}
		return list;
	}
	
	public String[] tagsAsArray()
	{
		return tagsAsList().toArray(new String[0]);
	}
}

