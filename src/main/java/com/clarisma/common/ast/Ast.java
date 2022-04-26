package com.clarisma.common.ast;

public abstract class Ast
{
	public abstract <R> R accept(AstVisitor<R> visitor); 
}
