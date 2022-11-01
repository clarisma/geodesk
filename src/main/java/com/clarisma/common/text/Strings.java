/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.text;

import java.io.PrintStream;
import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.StringJoiner;
import java.util.function.Function;

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
    		return Character.MAX_VALUE;
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

	// This method is much slower than building a new String via StringBuffer
	/*
	public static void printEscaped (PrintStream out, String s)
	{
		for (int i=0; i<s.length(); i++)
		{
			char ch = s.charAt(i);
			char chEscaped = escape(ch);
			if(chEscaped != Character.MAX_VALUE)
			{
				out.append('\\');
				out.append(chEscaped);
			}
			else
			{
				out.append (ch);
			}
		}
	}
	*/

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

    /**
     * Checks if a string represents a valid number, and if so,
     * whether it is integral or floating-point.
     * 
     * @param s 	the string to check
     * @return		`Double.class` if the string is fractional,
     * 				`Long.class` if integral, or `null` if it
     * 				is not a valid number.
     */
    public static Type numberType(CharSequence s)
    {
    	s=s.toString().trim();
    	boolean minus = false;
		boolean decimal = false;
		boolean digits = false;
		int n=0;
		for(; n < s.length(); n++)
		{
			char ch = s.charAt(n);
			if (ch=='-')
			{
				if (minus || digits) return null;
				minus=true;
				continue;
			}
			if (ch=='.')
			{
				if (decimal) return null;
				decimal=true;
				continue;
			}
			if(Character.isDigit(ch)) 
			{
				digits=true;
				continue;
			}
			return null;
		}
		if (!digits) return null;
		return decimal ? Double.class : Long.class;
    }
    
    public static boolean isAsciiLetter(char ch)
    {
    	return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'); 
    }
    
    public static boolean isIdentifier(String s)
    {
    	for(int i=0; i<s.length(); i++)
    	{
    		char ch = s.charAt(i);
    		if(i==0)
    		{
    			if(!isAsciiLetter(ch) && ch != '_') return false;
    		}
    		else
    		{
    			if(!isAsciiLetter(ch) &&
    				!Character.isDigit(ch) &&
    				ch != '_') return false;
    		}
    	}
    	return true;
    }
    
    public static boolean equals(String a, String b)
    {
    	if(a==null || b==null) return a==b;
    	return a.equals(b);
    }
    
    /**
     * Formats a floating-point number. Unlike Double.toString,
     * omits the decimal point if this number is not fractional.
     * 
     * @param d
     * @return
     */
    public static String formatSimpleDouble(double d)
    {
    	int i = (int)d;
		return i==d ? Integer.toString(i) : Double.toString(d);
    }
    
    /**
     * Turns a string into a conforming string key, which is
     * always lowercase and only contains letters, numbers, and
     * hyphens.
     * 
     * TODO: examples
     *  
     * "Kirchäckerstraße" ==> "kirchaeckerstrasse"
     *  
     * @param s
     * @return
     */
    public static String makeKey(String s)
    {
    	StringBuffer buf = new StringBuffer();
    	s = stripAccents(s);
    	boolean needsHyphen = false;
    	for(int i=0; i<s.length(); i++)
    	{
    		char ch = s.charAt(i);
    		if(Character.isLetter(ch))
    		{
    			if(needsHyphen)
    			{
    				buf.append('-');
    				needsHyphen=false;
    			}
    			ch = Character.toLowerCase(ch);
    			buf.append(ch);
    			continue;
    		}
    		if(Character.isDigit(ch))
    		{
    			if(needsHyphen)
    			{
    				buf.append('-');
    				needsHyphen=false;
    			}
    			buf.append(ch);
    			continue;
    		}
    		if(buf.length() > 0) needsHyphen = true;
    	}
    	return buf.toString();
    }
    
    public static boolean isAscii(String s)
    {
    	for(int i=0; i<s.length(); i++)
    	{
    		if(s.charAt(i) >= 128) return false;
    	}
    	return true;
    }
 
    public static String stripAccents(String original)
    {
    	String s = Normalizer.normalize(original, Normalizer.Form.NFD);
    	// if(s.equals(original)) return original;
    	if (isAscii(s)) return original;
    	StringBuffer buf = new StringBuffer();
    	boolean uppercase = false;
    	for(int i=0; i<s.length(); i++)
    	{
    		char ch = s.charAt(i);
    		switch(ch)
    		{
    		case '\u0308':	
    			buf.append(uppercase ? 'E' : 'e');
    			break;
    		case 'æ':
    			buf.append("ae");
    			break;
    		case 'Æ':
    			buf.append("AE");
    			break;
    		case 'œ':
    			buf.append("oe");
    			break;
    		case 'Œ':
    			buf.append("OE");
    			break;
    		case 'Þ':
    			buf.append("th");
    			break;
    		case 'ð':
    			buf.append("d");
    			break;
    		case 'ß':
    			buf.append("ss");
    			break;
    		default:
    			if(ch < 128) 
    			{
    				buf.append(ch);
    				uppercase = Character.isUpperCase(ch);
    			}
    			break;
    		}
    	}
    	// TODO: leading/trailing dash, d' 
    	return buf.toString();
    }

    public static int longestCommonSubstring(String a, String b)
    {
    	int m = a.length();
    	int n = b.length();
     
    	int max = 0;
     
    	int[][] dp = new int[m][n];
     
    	for(int i=0; i<m; i++)
    	{
    		for(int j=0; j<n; j++)
    		{
    			if(a.charAt(i) == b.charAt(j))
    			{
    				if(i==0 || j==0)
    				{
    					dp[i][j]=1;
    				}
    				else
    				{
    					dp[i][j] = dp[i-1][j-1]+1;
    				}
     				if(max < dp[i][j]) max=dp[i][j];
    			}
     		}
    	}
    	return max;
    }

	// TODO: remove
    public static <T> String join(String delimiter, Iterable<T> items)
    {
    	StringJoiner joiner = new StringJoiner(delimiter);
    	for(T item: items) joiner.add(item==null ? "null" : item.toString());
    	return joiner.toString();
    }

	// TODO: remove
    public static <T> String join(String delimiter, Iterable<T> items, Function<T, Object> formatter)
    {
    	StringJoiner joiner = new StringJoiner(delimiter);
    	for(T item: items) joiner.add(String.valueOf(formatter.apply(item)));
    	return joiner.toString();
    }
    
    public static int indexOfAny(String s, String any)
    {
    	for(int i=0; i<s.length(); i++)
    	{
    		if(any.indexOf(s.charAt(i)) >= 0) return i;
    	}
    	return -1;
    }
    
    public static int countChar(String s, char ch)
    {
    	int count = 0;
    	for(int i=0; i<s.length(); i++)
    	{
    		if(s.charAt(i)==ch) count++;
    	}
    	return count;
    }

	public static String uppercaseFirst(String s)
	{
		if(s.isEmpty()) return s;
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
