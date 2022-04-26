package com.clarisma.common.pbf;

public class PbfException extends RuntimeException 
{
	public PbfException (String msg)
	{
		super(msg);
	}

	public PbfException (String msg, Exception root)
	{
		super(msg, root);
	}
}
