package com.clarisma.common.fab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

// TODO: Use simple = instead of := for literal values without comments and sub-keys

public class FabReader 
{
	protected int tabSize = 4;
	private static final int MAX_NESTING_LEVELS = 32;
	private final int[] indentStack;
	private int currentNestingLevel;
	/**
	 * Key which has not been dispatched.
	 */
	private String openKey;
	/**
	 * Value that has not been dispatched.
	 */
	private StringBuilder openValue;
	protected String fileName;
	/**
	 * The current line (1-based)
	 */
	protected int lineNumber;
	/**
	 * The key in the current line, or null if none
	 */
	private String key;
	/**
	 * The value in the current line, or null if none
	 */
	private String value;
	/**
	 * the column at which the key or value begins in the current line,
	 * or -1 if empty line
	 */
	private int lineIndent;
	/**
	 * The column at which the last key appeared
	 */
	private int keyIndent;
	/**
	 * If a key's value is spread over multiple lines, this is the column
	 * where the values should line up. If a value appears to the right,
	 * the extra whitespace becomes part of the value.
	 * A value may not appear to the left
	 * -1 if no multi-line value has been seen yet
	 */
	private int valueIndent;
	/**
	 * If true, keys and comments in the lines following a key are ignored,
	 * and instead are included as part of the value (This makes it possible
	 * to have a text value that includes a colon followed by a space)
	 */
	private boolean literalMode;

	public FabReader()
	{
		openValue = new StringBuilder();
		indentStack = new int[MAX_NESTING_LEVELS];
		valueIndent = -1;
	}
	
	protected void beginKey(String key, String value)
	{
		beginKey(key);
		keyValue("value", value);
	}
	
	protected void beginKey(String key)
	{
		// System.out.format("BEGIN [%s]\n", key);
	}
	
	protected void keyValue(String key, String value)
	{
		System.out.format("VALUE [%s] = [%s]\n", key, value);
	}
	
	protected void endKey()
	{
		// System.out.format("END\n");
	}
	
	protected void error(String msg)
	{
		throw new FabException(
			String.format("%s:%d: %s",
			fileName==null ? "<none>" : fileName,
			lineNumber, msg));	
	}
	
	protected void error(String msg, Object... args)
	{
		error(String.format(msg, args));
	}
	
	protected int toInt(String s)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch(NumberFormatException ex)
		{
			error("Expected number instead of %s", s);
			return 0;
		}
	}
	
	private void parseLine(String line)
	{
		int pos=0;
		lineIndent = 0;
		for(; pos<line.length(); pos++)
		{
			char ch = line.charAt(pos);
			if(ch=='\t')
			{
				lineIndent += tabSize-(lineIndent % tabSize);
				continue;
			}
			if (!Character.isWhitespace(ch)) break;
			lineIndent++;
		}
		int valueStart = pos;
		int valueEnd = line.length();
		int keyStart = pos;
		int keyEnd = -1;
		if(lineIndent < valueIndent) literalMode = false;
		if(!literalMode)
		{
			for(; pos<line.length(); pos++)
			{
				char ch = line.charAt(pos);
				if (keyEnd < 0 && ch==':')
				{
					if(pos == line.length()-1 ||
						Character.isWhitespace(
						line.charAt(pos+1)))
					{
						keyEnd = pos;
						pos++;
						literalMode = false;
						continue;
					}
					if(line.charAt(pos+1) == '=')
					{
						if(pos == line.length()-2 ||
							Character.isWhitespace(
							line.charAt(pos+2)))
						{
							keyEnd = pos;
							pos+=2;
							literalMode = true;
							continue;
						}
					}
				}
				if(ch == '/')
				{
					// Check for comment start:
					// "//" followed by whitespace
					int lineLen = line.length();
					if(pos > lineLen-2) break;
					if(line.charAt(pos+1) == '/' &&	(pos+2 == lineLen ||
						Character.isWhitespace(line.charAt(pos+2))))
					{
						valueEnd = pos;
						break;
					}
				}
			}
			if(keyEnd >= 0)
			{
				// If a key has been found, a potential
				// value starts after the key; 
				// trim leading whitespace
				valueStart = keyEnd+2;
				for(;valueStart < line.length(); valueStart++)
				{
					char ch = line.charAt(valueStart);
					if(!Character.isWhitespace(ch)) break;
				}
			}
			// trim trailing whitespace 
			for(;valueEnd > 0; valueEnd--)
			{
				char ch = line.charAt(valueEnd-1);
				if(!Character.isWhitespace(ch)) break;
			}
		}
		key = (keyEnd > keyStart) ? 
			line.substring(keyStart, keyEnd) : null;
		value = (valueEnd > valueStart) ? 
			line.substring(valueStart, valueEnd) : null;
		if(key==null && value==null) lineIndent = -1;
		if(key != null)
		{
			valueIndent = -1;
		}
		else if (valueIndent < 0)
		{
			valueIndent = lineIndent;
		}
	}
	
	private void pushIndent()
	{
		indentStack[currentNestingLevel] = keyIndent;
		currentNestingLevel++;
	}
	
	private void popIndent(int targetIndent) 
	{
		//System.out.println("currentNestingLevel="+currentNestingLevel);
		while(currentNestingLevel>0 && keyIndent != targetIndent)
		{
			currentNestingLevel--;
			keyIndent=indentStack[currentNestingLevel];
			endKey();
		}
		if(keyIndent != targetIndent)
		{
			error("Unexpected indentation");
		}
	}

	/**
	 * Dispatches a pending key or key/value.
	 *
	 * @param keepOpen   if true, begins a block, else
	 *                   simply dispatches a key/value
	 */
	private void dispatch(boolean keepOpen)
	{
		if(openValue.length() > 0)
		{
			String val = openValue.toString().trim();
			if(keepOpen)
			{
				beginKey(openKey, val);
			}
			else
			{
				keyValue(openKey, val);
			}
			openValue.setLength(0);
			valueIndent = -1;
			openKey = null;
		}
		else if(openKey != null)
		{
			if(keepOpen)
			{
				beginKey(openKey);
			}
			else
			{
				keyValue(openKey, "");
			}
		}
	}
		
	public void read(BufferedReader in) throws FabException, IOException
	{
		for(;;)
		{
			lineNumber++;
			String line = in.readLine();
			if(line==null) break;
			parseLine(line);
			if(key != null)
			{
				if(lineIndent < keyIndent)
				{
					dispatch(false);
					popIndent(lineIndent);
				}
				else if (lineIndent > keyIndent)
				{
					dispatch(true);
					pushIndent();
				}
				else
				{
					dispatch(false);
				}
				openKey = key;
				keyIndent = lineIndent;
				if (value != null) openValue.append(value);
			}
			else if (value != null)
			{
				if(lineIndent <= keyIndent ||
					lineIndent < valueIndent ||
					openKey == null)
				{
					error("Expected key");
				}
				/*
				else if(keyIndent == valueIndent)
				{
					dispatch(false);
					popIndent(lineIndent);
				}
				*/
				else
				{
					if(openValue.length() > 0) openValue.append('\n');
					if(lineIndent > valueIndent)
					{
						int padding = lineIndent-valueIndent;
						for(int i=0; i<padding; i++) openValue.append(' ');
					}
					openValue.append(value);
				}
			}
		}
		dispatch(false);
		popIndent(0);
	}

	public void read(InputStream in) throws FabException, IOException
	{
		read(new BufferedReader(new InputStreamReader(in)));
	}
	
	public void readFile(String fileName) throws FabException, IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(
			fileName));
		read(in);
		in.close();
	}
	
	public void readFile(File file) throws FabException, IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(file));
		read(in);
		in.close();
	}

	public void readFile(Path path) throws FabException, IOException
	{
		readFile(path.toFile());
	}
}
