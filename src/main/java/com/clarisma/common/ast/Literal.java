/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.ast;

import java.util.Objects;

public class Literal extends Expression
{
	private final Object value;
	
	public Literal(Object value)
	{
		this.value = value;
	}
	
	public Object value()
	{
		return value;
	}

	@Override public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Literal literal = (Literal) o;
		return Objects.equals(value, literal.value);
	}

	@Override public int hashCode()
	{
		return Objects.hash(value);
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitLiteral(this);
	}

	/*
	@Override public Object clone()
	{
		Literal v = (Literal)super.clone();
		if(value instanceof Cloneable) v.value = value.c
		v.operand = (Expression)operand.clone();
		return v;
	}
	 */

}
