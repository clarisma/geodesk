/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
