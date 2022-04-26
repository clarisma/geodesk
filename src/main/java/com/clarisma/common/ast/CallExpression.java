package com.clarisma.common.ast;

public class CallExpression extends Expression
{
	private final Expression callee;
	private final Expression[] arguments;

	
	public CallExpression(Expression callee, Expression[] arguments)
	{
		this.callee = callee;
		this.arguments = arguments;
	}


	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		return visitor.visitCall(this);
	}
}
