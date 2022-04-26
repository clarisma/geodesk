package com.clarisma.common.fab;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PropertyFabReader extends FabReader 
{
	private Map<String,String> properties = new HashMap<>();
	private String prefix;

	protected void beginKey(String key, String value)
	{
		if(prefix != null) key = prefix + key;
		properties.put(key, value);
		prefix = key + ".";
	}
	
	protected void beginKey(String key)
	{
		if(prefix != null) key = prefix + key;
		prefix = key + ".";
	}
	
	protected void keyValue(String key, String value)
	{
		if(prefix != null) key = prefix + key;
		properties.put(key, value);
	}
	
	protected void endKey()
	{
		assert prefix != null;
		int n = prefix.lastIndexOf('.', prefix.length()-2);
		assert n > 0;
		prefix = prefix.substring(0, n+1);
	}
	
	public Map<String,String> properties() 
	{ 
		return properties; 
	}
	
	public Map<String,String> readProperties(String filename) 
		throws FabException, IOException
	{
		readFile(filename);
		return properties;
	}
}
