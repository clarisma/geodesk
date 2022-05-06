package com.geodesk.feature.filter;

public class QueryException extends RuntimeException
{
	public QueryException(String msg)
	{
		super(msg);
	}
	
	public QueryException(String msg, Exception ex)
	{
		super(msg, ex);
	}
}