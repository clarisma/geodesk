package com.clarisma.common.ast;

import java.util.Objects;

public class BinaryExpression extends Expression
{
	private final Operator operator;
	private final Expression left;
	private final Expression right;
	
	public BinaryExpression(Operator operator, Expression left, Expression right)
	{
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	@Override public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BinaryExpression that = (BinaryExpression) o;
		return Objects.equals(operator, that.operator) && Objects.equals(left,
			that.left) && Objects.equals(right, that.right);
	}

	/*
	@Override public Object clone()
	{
		BinaryExpression v = (BinaryExpression)super.clone();
		v.left = (Expression)left.clone();
		v.right = (Expression)right.clone();
		return v;
	}
	 */

	@Override public int hashCode()
	{
		return Objects.hash(operator, left, right);
	}

	public Operator operator()
	{
		return operator;
	}
	
	public Expression left()
	{
		return left;
	}

	public Expression right()
	{
		return right;
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitBinary(this);
	}
}
