package com.clarisma.common.ast;

public class ConditionalExpression extends Expression
{
	private final Expression condition;
	private final Expression ifTrue;
	private final Expression ifFalse;

	public ConditionalExpression(Expression condition, Expression ifTrue, Expression ifFalse)
	{
		this.condition = condition;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
	}

	public Expression condition()
	{
		return condition;
	}

	public Expression ifTrue()
	{
		return ifTrue;
	}

	public Expression ifFalse()
	{
		return ifFalse;
	}

	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitConditional(this);
	}
}
