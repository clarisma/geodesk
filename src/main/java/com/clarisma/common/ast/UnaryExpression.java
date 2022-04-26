package com.clarisma.common.ast;

import java.util.Objects;

public class UnaryExpression extends Expression
{
	private final Operator operator;
	private final Expression operand;
	
	public UnaryExpression(Operator operator, Expression operand)
	{
		this.operator = operator;
		this.operand = operand;
	}

	public Operator operator()
	{
		return operator;
	}

	public Expression operand()
	{
		return operand;
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitUnary(this);
	}

	@Override public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UnaryExpression that = (UnaryExpression) o;
		return Objects.equals(operator, that.operator) &&
			Objects.equals(operand, that.operand);
	}

	/*
	@Override public Object clone()
	{
		UnaryExpression v = (UnaryExpression)super.clone();
		v.operand = (Expression)operand.clone();
		return v;
	}
	 */

	@Override public int hashCode()
	{
		return Objects.hash(operator, operand);
	}
}
