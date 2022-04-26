package com.clarisma.common.ast;

import java.io.OutputStream;

import com.clarisma.common.xml.XmlWriter;

public class ExpressionXmlWriter extends XmlWriter implements AstVisitor<Void>
{
	public ExpressionXmlWriter(OutputStream out)
	{
		super(out);
	}

	@Override public Void visitBinary(BinaryExpression exp)
	{
		begin(exp.operator().name());
		exp.left().accept(this);
		exp.right().accept(this);
		end();
		return null;
	}

	@Override public Void visitUnary(UnaryExpression exp)
	{
		begin(exp.operator().name());
		exp.operand().accept(this);
		end();
		return null;
	}

	@Override public Void visitString(StringExpression exp)
	{
		// TODO
		return null;
	}

	@Override public Void visitLiteral(Literal exp)
	{
		Object val = exp.value();
		// begin(ClassUtils.getShortClassName(val.getClass()).toLowerCase());
		begin(val.getClass().getSimpleName().toLowerCase());
		attr("value", val);
		end();
		return null;
	}

	@Override public Void visitVariable(Variable exp)
	{
		begin("var");
		attr("name", exp.name());
		end();
		return null;
	}

	@Override public Void visitCall(CallExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Void visitConditional(ConditionalExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Void visitExpression(Expression exp)
	{
		// unknown expression; do nothing 
		return null;
	}

}
