/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.ast;

public class StringExpression extends Expression
{
	private final Expression[] parts;
	
	public StringExpression(Expression[] parts)
	{
		this.parts = parts;
	}

	public Expression[] parts()
	{
		return parts;
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitString(this);
	}
}
