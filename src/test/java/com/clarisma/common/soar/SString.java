package com.clarisma.common.soar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.clarisma.common.pbf.PbfEncoder;

public class SString extends SharedStruct implements Comparable<SString> 
{
	private String string;
	private byte[] bytes;
	
	public SString(String s)
	{
		string = s;
		bytes = s.getBytes(StandardCharsets.UTF_8);
		int lenLength = PbfEncoder.varintLength(bytes.length);
		setSize(bytes.length + lenLength);
		setAlignment(0);
	}

	@Override public boolean equals(Object other)
	{
		if(!(other instanceof SString)) return false;
		return string.equals(((SString)other).string);
	}

	@Override public int hashCode()
	{
		return string.hashCode();
	}

	@Override public String toString()
	{
		return string;
	}

	@Override public String dumped()
	{
		// return String.format("STRING \"%s\"%s", string, sharedSuffix());
		return String.format("STRING \"%s\"", string);
	}

	@Override public int compareTo(SString other)
	{
		return string.compareTo(other.string);
	}

	@Override public void writeTo(StructOutputStream out) throws IOException
	{
		out.writeVarint(bytes.length);
		out.write(bytes);
	}
}
