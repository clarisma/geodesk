/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.ast;


public class TypeChecker implements AstVisitor<Class<?>>
{
	public Class<?> getType(Expression exp)
	{
		return exp.accept(this);
	}
	
	@Override public Class<?> visitBinary(BinaryExpression exp)
	{
		Operator op = exp.operator();
		if(op == Operator.ADD || op == Operator.OR)
		{
			return Boolean.class;
		}
		
		// TODO
		
		return null;
	}

	@Override public Class<?> visitUnary(UnaryExpression exp)
	{
		Operator op = exp.operator();
		if(op == Operator.NOT) return Boolean.class;
		return getType(exp.operand());
	}

	@Override public Class<?> visitString(StringExpression exp)
	{
		return String.class;
	}

	@Override public Class<?> visitLiteral(Literal exp)
	{
		// TODO: should we return the primitive type for wrapper classes?
		Class<?> c = exp.value().getClass();
		if(c == Double.class) return Double.TYPE;	// TODO
		return c;
	}

	protected Class<?> commonType(Class<?> a, Class<?> b)
	{
		if(a == b) return a;
		if(a == null) return b;
		if(b == null) return a;
		return Double.TYPE;
	}

	protected Class<?> commonType(Expression a, Expression b)
	{
		return commonType(getType(a), getType(b));
	}

	@Override public Class<?> visitVariable(Variable exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Class<?> visitCall(CallExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Class<?> visitConditional(ConditionalExpression exp)
	{
		return commonType(getType(exp.ifTrue()), getType(exp.ifFalse()));
	}

	@Override public Class<?> visitExpression(Expression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
