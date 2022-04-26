package com.clarisma.common.parser;

import java.util.HashMap;
import java.util.Map;

public class JsonParser extends Parser
{
	public static final String COMMA = ",";
	public static final String COLON = ":";
	public static final String LBRACE = "{";
	public static final String RBRACE = "}";
	public static final String LBRACKET = "[";
	public static final String RBRACKET = "]";
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String NULL = "null";

	protected static final Object TOKENS[] =
	{
		COMMA, COLON, LBRACE, RBRACE, LBRACKET, RBRACKET,
		TRUE, FALSE, NULL
	};

	public JsonParser()
	{
		for(Object tok : TOKENS) 
		{
			addToken(tok.toString(), tok);
		}
	}
	
	public Object value()
	{
		if(tokenType == LBRACE)
		{
			return object();
		}
		if(tokenType == STRING)
		{
			Object value = unquotedStringValue();
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
		if(tokenType==NULL)
		{
			nextToken();
			return null;		// TODO: differentiate from error
		}
		
		// TODO: arrays
		
		return null;
	}
	
	public Map<String,Object> object()
	{
		if(!acceptAndConsume(LBRACE)) return null;
		Map<String, Object> map = new HashMap<>();
		
		for(;;)
		{
			if(tokenType==RBRACE) return map;
			String key;
			if(tokenType == IDENTIFIER)
			{
				key = stringValue();
			}
			else if (tokenType == STRING)
			{
				key = unquotedStringValue();
			}
			else
			{
				error("Expected identifier or }");
				return null;
			}
			expect(COLON);
			nextToken();
			map.put(key, value());
			if(!acceptAndConsume(COMMA)) expect(RBRACE);
		}
	}
}
