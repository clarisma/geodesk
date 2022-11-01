/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.fab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FabWriter 
{
	private Appendable out;
	private int tabSize = 4;
	private Item current;
	private Stack<Item> stack;
	
	private static class Item
	{
		public String key;
		public String value;
		public String comment;
		public List<Item> children;
		
		public Item(String k, String v, String c)
		{
			key = k;
			value = v;
			comment = c;
		}
		
		public void add(Item item)
		{
			if(children==null) children = new ArrayList<>();
			children.add(item);
		}
	}
	
	public FabWriter(Appendable out)
	{
		this.out = out;
		current = new Item(null, null, null);
		stack = new Stack<>();
	}
	
	public void beginKey(String key)
	{
		beginKey(key, null, null);
	}
	
	public void beginKey(String key, String value, String comment)
	{
		Item item = keyValue(key,value,comment);
		stack.push(current);
		current=item;
	}
	
	public Item keyValue(String key, String value)
	{
		return keyValue(key, value, null);
	}
	
	public Item keyValue(String key, String value, String comment)
	{
		Item item = new Item(key, value, comment);
		current.add(item);
		return item;
	}
	
	public void endKey()
	{
		current = stack.pop();
	}
	
	private void indent(int count) throws IOException
	{
		for(int i=0; i<count; i++) out.append('\t');
	}

	private void writeItems(int level, List<Item> items) throws IOException
	{
		int keyWidth=0;
		int valWidth=0;
		for(int i=0; i<items.size(); i++)
		{
			Item item = items.get(i);
			int keyLen = item.key.length(); 
			int valLen = item.value==null ? 0 : item.value.length();
			if(keyLen > keyWidth) keyWidth = keyLen;
			if(valLen > valWidth) valWidth = valLen;
		}
		keyWidth+=2;	// for the ':' and space
		valWidth+=2;	// for two spaces 
		int padding = keyWidth % tabSize;
		if(padding > 0) keyWidth += tabSize-padding;
		padding = valWidth % tabSize;
		if(padding > 0) valWidth += tabSize-padding;
		for(int i=0; i<items.size(); i++)
		{
			Item item = items.get(i);
			String value = item.value;
			indent(level);
			out.append(item.key);
			out.append(':');
			if(value != null || item.comment != null)
			{
				padding = keyWidth-item.key.length()-1;
				indent((padding+tabSize-1) / tabSize);
				if(value != null)
				{
					// TODO: multi-line
					out.append(value);
				}
				if(item.comment != null)
				{
					padding = valWidth - (value == null ? 0 : value.length());
					indent((padding+tabSize-1) / tabSize + 1);
					out.append(item.comment);
				}
			}
			out.append('\n');
			if(item.children != null)
			{
				writeItems(level+1, item.children);
			}
		}
	}
					
	public void endDocument() throws IOException
	{
		assert stack.size()==0;
		if(current.children != null) writeItems(0, current.children);
		current.children=null;
	}
}
