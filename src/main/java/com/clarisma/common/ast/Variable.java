/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.ast;

public class Variable extends Expression
{
	private final String name;
	
	public Variable(String name)
	{
		this.name = name;
	}
	
	public String name()
	{
		return name;
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitVariable(this);
	}
}
