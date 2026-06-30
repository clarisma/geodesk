/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.text;

public class Strings
{
	/**
	 * Checks if a character needs to be escaped, and if so,
	 * returns its corresponding escape character. When escaping
	 * entire strings, escape characters need to be preceded 
	 * by a backslash.
	 *  
	 * @param ch	the character to check
	 * @return		its corresponding escape character, or
	 * 				`Character.MAX_VALUE` if this character
	 * 				does not require escaping
	 */
	public static char escape(char ch)
    {
    	if(ch < 32)
    	{
    		switch(ch)
    		{
    			case '\b': return 'b';
    			case '\n': return 'n';
    			case '\t': return 't';
    			case '\f': return 'f';
    			case '\r': return 'r';
    			case '\0': return '0';
    		}
    		return Character.MAX_VALUE;	// TODO: check
    	}
    	switch(ch)
    	{
    	case '\'':
    	case '\"':
    	case '\\':
    		return ch;
    	}
    	return Character.MAX_VALUE;
    }

	/**
	 * Turns an escape character into its true character.
	 * 
	 * @param ch	the escape character
	 * @return		the actual character, or `Character.MAX_VALUE`
	 * 				if `ch` does not represent a valid escape
	 * 				character. 
	 */
    public static char unescape(char ch)
    {
        switch(ch)
        {
        case 'b': return '\b';
        case 'n': return '\n';
        case 't': return '\t';
        case 'f': return '\f';
        case 'r': return '\r';
        case '\'': return '\'';
        case '\"': return '\"';
        case '\\': return '\\';
        case '0': return '\0';
        }
        return Character.MAX_VALUE;
    }

    public static String escape (String s)
    {
    	StringBuilder buf = new StringBuilder ();
        for (int i=0; i<s.length(); i++)
        {
            char ch = s.charAt(i);
            char chEscaped = escape(ch);
            if(chEscaped != Character.MAX_VALUE)
            {
            	buf.append('\\');
                buf.append(chEscaped);
            }
            else
            {
            	buf.append (ch);
            }
        }
        return buf.toString ();
    }

	public static String unescape (String s, boolean trimQuotes)
    {
    	StringBuilder buf = new StringBuilder ();
    	int len = s.length() - (trimQuotes ? 1 : 0);
    	int i = trimQuotes ? 1 : 0;
        for (; i<len; i++)
        {
        	char ch = s.charAt(i);
        	if (ch=='\\')
        	{
        		i++;
				if(i >= len) break;
				buf.append(unescape(s.charAt(i)));
        	}
        	else
        	{
        		buf.append(ch);
        	}
        }
        return buf.toString();
    }
}
