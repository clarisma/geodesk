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
