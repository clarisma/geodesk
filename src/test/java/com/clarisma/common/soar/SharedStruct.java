package com.clarisma.common.soar;

import java.io.PrintWriter;

public abstract class SharedStruct extends Struct
{
	private int userCount;
	private float usage;
	
	public float usage()
	{
		return usage;
	}
	
	public int userCount()
	{
		return userCount;
	}
	
	public void addUsage(float u)
	{
		usage += u;
		userCount++;
	}
	
	public void addUsage(int users, float u)
	{
		usage += u;
		userCount += users;
	}
	
	protected String sharedSuffix()
	{
		// return userCount > 1 ? String.format(" (%d: %.0f)", userCount, usage) : "";
		return String.format(" [%d: %.0f]", userCount, usage);
	}

	public void dump(PrintWriter out)
	{
		out.format("%08X  %s%s\n", location(), dumped(), sharedSuffix());
	}
	
	public boolean isShared()
	{
		return userCount > 1;
	}
}
