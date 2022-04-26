package com.clarisma.common.ast;

public interface AstVisitor<R>
{
	R visitExpression(Expression exp);
	R visitBinary(BinaryExpression exp);
	R visitUnary(UnaryExpression exp);
	R visitString(StringExpression exp);
	R visitLiteral(Literal exp);
	R visitVariable(Variable exp);
	R visitCall(CallExpression exp);
	R visitConditional(ConditionalExpression exp);
}
