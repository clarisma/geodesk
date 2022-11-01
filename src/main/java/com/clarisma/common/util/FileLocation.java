/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
