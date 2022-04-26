package com.clarisma.common.fab;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SaxFabReader extends FabReader
{
	private ContentHandler handler;
	private static final Attributes EMPTY_ATTR = new AttributesImpl();
	private Stack<String> elements;
	
	protected void saxException(SAXException ex)
	{
		error("Encountered SAX exception: " + ex.getMessage());
	}
	
	protected void beginKey(String key, String value) 
	{
		try 
		{
			handler.startElement(null, key, key, EMPTY_ATTR);
			elements.push(key);
			handler.characters(value.toCharArray(), 0, value.length());
		}
		catch (SAXException ex) 
		{
			saxException(ex);
		}
	}
	
	protected void beginKey(String key)
	{
		try
		{
			handler.startElement(null, key, key, EMPTY_ATTR);
			elements.push(key);
		}
		catch (SAXException ex) 
		{
			saxException(ex);
		}
	}
	
	protected void keyValue(String key, String value)
	{
		beginKey(key, value);
		endKey();
	}
	
	protected void endKey()
	{
		try
		{
			String key = elements.pop();
			handler.endElement(null, key, key);
		}
		catch (SAXException ex) 
		{
			saxException(ex);
		}
	}
	
	public void read(BufferedReader in, 
		String baseElement, ContentHandler handler) throws FabException, IOException
	{
		this.handler = handler;
		elements = new Stack<>();
		try
		{
			handler.startDocument();
			handler.startElement(null, baseElement, baseElement, EMPTY_ATTR);
			read(in);
			handler.endElement(null, baseElement, baseElement);
			handler.endDocument();
			this.handler = null;
			assert elements.isEmpty();
			elements = null;
		}
		catch(SAXException ex)
		{
			saxException(ex);
		}
	}
}
