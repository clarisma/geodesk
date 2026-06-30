/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.xml;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;

// TODO: fix or scrap
public class XmlWriter extends PrintWriter
{
    private String indentString = "  ";
    private Stack<String> elements;
    private boolean childElements = true;
    // private Escaper escaper;

    public XmlWriter(OutputStream out)
    {
        super(out);
        elements = new Stack<>();
        println("<?xml version='1.0' encoding='UTF-8'?>");
    }

    protected void indent()
    {
        for(int i=0; i<elements.size(); i++) print(indentString);
    }

    public void begin(String tag)
    {
        if(!childElements)
        {
            println(">");
        }
        indent();
        print("<");
        print(tag);
        elements.push(tag);
        childElements = false;
    }

    public void attr(String a, Object v)
    {
        print(' ');
        print(a);
        print("=\"");
        print(escape(v.toString()));
        print('\"');
    }

    public void attr(String a, long v)
    {
        print(' ');
        print(a);
        print("=\"");
        print(v);
        print('\"');
    }

    public void end()
    {
        String tag = elements.pop();
        if(childElements)
        {
            indent();
            print("</");
            print(tag);
            println(">");
        }
        else
        {
            println("/>");
            childElements = true;
        }
    }

    public String escape(String s)
    {
        return s; // TODO
    }

    public void empty(String elem, Object... args)
    {
        if(!childElements)
        {
            println(">");
            childElements = true;
        }
        indent();
        print("<");
        format(elem, args);
        println("/>");
    }
}
