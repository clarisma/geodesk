/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.fab;

public class EchoFabReader extends FabReader
{
	private int level;
	
	protected void indent()
	{
		for(int i=0;i<level;i++)
		{
			System.out.print("   ");
		}
	}
	
	protected void beginKey(String key, String value)
	{
		keyValue(key, "");
		level++;
		keyValue("value", value);
	}
	
	protected void beginKey(String key)
	{
		indent();
		System.out.format("%s:\n", key);
		level++;
	}
	
	protected void keyValue(String key, String value)
	{
		indent();
		System.out.format("%s: %s\n", key, value);
	}
	
	protected void endKey()
	{
		level--;
	}
	
	protected void error(String msg) 
	{
		System.out.format("ERROR: %s:%d: %s\n",
			fileName==null ? "<none>" : fileName,
			lineNumber, msg);	
	}

}
