/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import com.clarisma.common.ast.Expression;
import com.clarisma.common.ast.ExpressionXmlWriter;
import com.clarisma.common.ast.Literal;
import com.clarisma.common.ast.Variable;

import java.io.OutputStream;

public class MatcherXmlWriter extends ExpressionXmlWriter
{
	boolean inTagClause;
	
	public MatcherXmlWriter(OutputStream out)
	{
		super(out);
	}
	
	@Override public Void visitLiteral(Literal exp)
	{
		Object val = exp.value();
		if(val instanceof GlobalString)
		{
			GlobalString cs = (GlobalString)val;
			begin("globalString");
			attr("value", cs.value());
			attr("string", cs.stringValue());
			end();
			return null;
		}
		return super.visitLiteral(exp);
	}
	
	@Override public Void visitVariable(Variable exp)
	{
		if(exp instanceof TagClause && !inTagClause)
		{
			inTagClause = true;
			TagClause clause = (TagClause)exp;
			begin("tag");
			attr("name", clause.name());
			attr("code", clause.keyCode());
			attr("required", clause.isKeyRequired());
			Expression tagExp = clause.expression();
			if(tagExp != null) tagExp.accept(this);
			end();
			inTagClause = false;
			return null;
		}
		return super.visitVariable(exp);
	}

	@Override public Void visitExpression(Expression exp)
	{
		if(exp instanceof Selector)
		{
			Selector sel = (Selector)exp;
			begin("selector");
			int types = sel.matchTypes();
			attr("nodes", (types & TypeBits.NODES) == TypeBits.NODES);
			attr("ways", (types & TypeBits.NONAREA_WAYS) == TypeBits.NONAREA_WAYS);
			attr("areas", (types & TypeBits.AREAS) == TypeBits.AREAS);
			attr("relations", (types & TypeBits.NONAREA_RELATIONS) == TypeBits.NONAREA_RELATIONS);
			TagClause clause = sel.firstClause();
			while(clause != null)
			{
				clause.accept(this);
				clause = clause.next();
			}
			end();
		}
		return null;
	}
	
	public void writeQuery(Selector sel)
	{
		begin("query");
		while(sel != null)
		{
			sel.accept(this);
			sel = sel.next();
		}
		end();
	}
}
