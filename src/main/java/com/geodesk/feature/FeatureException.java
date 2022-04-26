package com.geodesk.feature;

// TODO: needed?
public class FeatureException extends RuntimeException
{
	public FeatureException(String msg)
	{
		super(msg);
	}
	
	public FeatureException(String msg, Exception ex)
	{
		super(msg, ex);
	}
}
