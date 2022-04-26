package com.clarisma.common.util;

// TODO: change to TextLocation, move to ccc.text?
public interface FileLocation 
{
    String getFile ();
    int getLine ();
    default int getColumn () 
    { 
    	return -1; 
    }
}
