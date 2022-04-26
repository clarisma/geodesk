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
