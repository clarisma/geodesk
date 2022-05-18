package com.geodesk.util;

import com.clarisma.common.text.Strings;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Map;

/**
 * Methods to generate JavaScript.
 */
public class JavaScript 
{
	public static void writeMap(Appendable out, Map<?,?> v) throws IOException
	{
		boolean first = true;
		out.append('{');
		for (Map.Entry<?,?> entry : v.entrySet())
		{
			if(first)
			{
				first=false;
			}
			else
			{
				out.append(',');
			}
			out.append(entry.getKey().toString());
			out.append(':');
			writeValue(out,entry.getValue());
		}
		out.append('}');
	}
	
	public static void writeArray(Appendable out, Object v) throws IOException
	{
		out.append('[');
		int len = Array.getLength(v);
		for(int i=0; i<len; i++)
		{
			if(i>0) out.append(',');
			writeValue(out,Array.get(v, i));
		}
		out.append(']');
	}
	
	public static void writeString(Appendable out, String s) throws IOException
	{
		out.append('\"');
		out.append(Strings.escape(s));
		out.append('\"');
	}
		
	public static void writeValue(Appendable out, Object v) throws IOException
	{
		if (v==null)
		{
			out.append("null");
		}
		else if (v instanceof String)
		{
			writeString(out, (String)v);
		}
		else if (v instanceof Map<?,?>)
		{
			writeMap(out,(Map<?,?>)v);
		}
		else if (v.getClass().isArray())
		{
			writeArray(out,v);
		}
		else
		{
			out.append(v.toString());
		}
	}

}
