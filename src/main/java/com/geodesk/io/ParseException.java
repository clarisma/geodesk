/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io;

// TODO: use a more general class
public class ParseException extends RuntimeException
{
    public ParseException(String msg)
    {
        super(msg);
    }
}
