/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.ast;

import static org.objectweb.asm.Opcodes.*;

import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.clarisma.common.bytecode.Coder;
import com.clarisma.common.bytecode.Instructions;

import java.util.HashMap;
import java.util.Map;

public class ExpressionCoder extends Coder implements AstVisitor<Void>
{
	protected TypeChecker typeChecker;

	/**
	 * A mapping of regex expression strings to the (static) field name
	 * of their Pattern class. (This Map is instantiated lazily)
	 */
	private Map<String,String> regexPatterns;

	public void setTypeChecker(TypeChecker typeChecker)
	{
		this.typeChecker = typeChecker;
	}

	@Override public Void visitBinary(BinaryExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Void visitUnary(UnaryExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Emits code to process a string template expression.
	 * The default implementation joins the template parts using a
	 * StringBuilder and leaves the resulting String on the stack.
	 * 
	 * @param exp the StringExpression
	 */
	@Override public Void visitString(StringExpression exp)
	{
		Expression[] parts = exp.parts();
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		evaluate(parts[0], String.class);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", 
			"<init>", "(Ljava/lang/String;)V", false);
		for(int i=1; i<parts.length; i++)
		{
			evaluate(parts[i], String.class);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", 
				"append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		}
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", 
			"toString", "()Ljava/lang/String;", false);
		return null;
	}

	@Override public Void visitLiteral(Literal exp)
	{
		Object value = exp.value();
		if(value instanceof Integer)
		{
			loadIntConstant((Integer)value);
			return null;
		}
		mv.visitLdcInsn(value);
		return null;
	}
	
	/**
	 * Emits code to load the result of an expression onto the 
	 * stack, coercing it into the specified type if necessary.
	 *  
	 * @param exp   the Expression
	 * @param type	the expected type
	 */
	protected void evaluate(Expression exp, Class<?> type)
	{
		// TODO
	}

	/**
	 * Emits code to load the {@link java.util.regex.Pattern) object for
	 * the given regex onto the stack. This method also generates the
	 * static initialization code to compile the `Pattern` class.
	 *
	 * @param regex
	 */
	protected void loadRegexPattern(String regex)
	{
		String patternField;
		if(regexPatterns == null)
		{
			regexPatterns = new HashMap<>();
			patternField = null;
		}
		else
		{
			patternField = regexPatterns.get(regex);
		}
		if(patternField == null)
		{
			patternField = "PATTERN" + regexPatterns.size();
			regexPatterns.put(regex, patternField);
			FieldVisitor fv = cw.visitField(
				ACC_PRIVATE | ACC_FINAL | ACC_STATIC, patternField,
				"Ljava/util/regex/Pattern;", null, null);
			fv.visitEnd();
			MethodVisitor staticMv = staticInitializer();
			staticMv.visitLdcInsn(regex);
			staticMv.visitMethodInsn(INVOKESTATIC, "java/util/regex/Pattern",
				"compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
			staticMv.visitFieldInsn(PUTSTATIC, className, patternField,
				"Ljava/util/regex/Pattern;");
		}
		mv.visitFieldInsn(GETSTATIC, className, patternField,
			"Ljava/util/regex/Pattern;");
	}

	/**
	 * Emits code to test a string value against a regex pattern.
	 * The Pattern object and the candidate string must have been placed
	 * on the stack (and will be removed by the match operation).
	 *
	 * One of `t` or `f` (but not both) must specify a jump target.
	 *
	 * @param t		  	 where to jump if pattern matched
	 * @param f       	 where to jump if pattern NOT matched
	 */
	protected void matchRegexPattern(Label t, Label f)
	{
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Pattern",
			"matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Matcher",
			"matches", "()Z", false);
		// The value returned from matches() is 1 (true) or 0 (false)
		// IFNE jumps if 1 (true), IFEQ jumps if 0 (false)
		mv.visitJumpInsn(t != null ? IFNE : IFEQ, t != null ? t : f);
	}

	/**
	 * Emits code to test a string value against a regular
	 * expression pattern.
	 * Exactly one label (t or f) must be non-null.
	 * 
	 * @param left		 the Expression which produces the string 
	 *                   value to be tested
	 * @param right      the Expression which produces the regex 
	 *                   pattern string
	 * @param t		  	 where to jump if pattern matched
	 * @param f       	 where to jump if pattern NOT matched
	 */
	protected void matchPattern(Expression left, Expression right, Label t, Label f)
	{
		if(right instanceof Literal literal)
		{
			loadRegexPattern(literal.value().toString());
		}
		else
		{
			// For now, the pattern must be a Literal
			assert false: "Not implemented";
		}
		evaluate(left,  String.class);
		matchRegexPattern(t, f);
	}
	
	/**
	 * Emits code to process a comparison expression.
	 * Exactly one of the labels t/f must be non-null.
	 * 
	 * @param opcode  the comparison (e.g. IF_ICMPEQ, IF_ICMPLT)
	 * @param left    the left-hand operand
	 * @param right   the right-hand operand
	 * @param t		  where to jump if comparison is true
	 * @param f       where to jump if comparison is false
	 */
	protected void comparison(int opcode, Expression left, Expression right, Label t, Label f)
	{
		int originalOpcode = opcode;
		Label label;
		if(t != null) 
		{
			label = t;
		}
		else
		{	
			label = f;
			opcode = Instructions.negate(opcode);
		}
		Class<?> type = typeChecker.commonType(left,  right);
		evaluate(left, type);
		evaluate(right, type);
		if(type.isPrimitive())
		{
			if(type == Integer.TYPE)
			{
				mv.visitJumpInsn(opcode, label);
				return;
			}
			if(type == Double.TYPE)
			{
				// Choose opcode so that comparison fails if NaN
				mv.visitInsn(originalOpcode==IF_ICMPGT ||
					originalOpcode==IF_ICMPGE ? DCMPL : DCMPG);
			}
			else if(type == Long.TYPE)
			{
				mv.visitInsn(LCMP);
			}
			else if(type == Float.TYPE)
			{
				// Choose opcode so that comparison fails if NaN
				mv.visitInsn(originalOpcode==IF_ICMPGT ||
					originalOpcode==IF_ICMPGE ? FCMPL : FCMPG);
			}
			else
			{
				// same as int for boolean, byte, char, short
				mv.visitJumpInsn(opcode, label);
				return;
			}
			// use generic comparison opcode (e.g. IFLT instead of IF_ICMPLT)
			// for non-int types
			mv.visitJumpInsn(Instructions.genericComparisonOpcode(opcode), label);
			return;
		}
		if(type == String.class)
		{
			stringComparison(opcode, label);
			return;
		}
		objectComparison(type, opcode, label);
	}

	/**
	 * Emits code to compare two strings, which must already be
	 * placed on the stack.
	 * 
	 * @param opcode the comparison opcode (IF_ICMPEQ, etc.)
	 * @param label  where to jump if comparison is true
	 */
	protected void stringComparison(int opcode, Label label)
	{
		if(opcode == IF_ICMPEQ || opcode == IF_ICMPNE)
		{
			// use String.equals() because it is faster
			// than String.compareTo()
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", 
				"equals", "(Ljava/lang/Object;)Z", false);
			// true is 1, false is 0, therefore we use IFNE
			// to jump if strings are equal, not IFEQ
			mv.visitJumpInsn(opcode == IF_ICMPEQ ? IFNE : IFEQ, label);
			return;
		}
		
		// TODO: should relative comparison coerce strings to numbers?
		// coerce if one of them is a number; already done in comparison()
		
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", 
			"compareTo", "(Ljava/lang/String;)I", false); 
		mv.visitJumpInsn(Instructions.genericComparisonOpcode(opcode), label);
	}

	/**
	 * Emits code to compare two objects, which must already be
	 * placed on the stack.
	 *
	 * @param type   the class of the objects
	 * @param opcode the comparison opcode (IF_ICMPEQ, etc.)
	 * @param label  where to jump if comparison is true
	 */
	protected void objectComparison(Class<?> type, int opcode, Label label)
	{
		String typeName = type.getName().replace('.', '/');

		if(opcode == IF_ICMPEQ || opcode == IF_ICMPNE)
		{
			mv.visitMethodInsn(INVOKEVIRTUAL, typeName,	"equals",
				String.format("(L%s;)Z", typeName), false);
			// true is 1, false is 0, therefore we use IFNE
			// to jump if objects are equal, not IFEQ
			mv.visitJumpInsn(opcode == IF_ICMPEQ ? IFNE : IFEQ, label);
			return;
		}
		mv.visitMethodInsn(INVOKEVIRTUAL, typeName, "compareTo",
			String.format("(L%s;)I", typeName), false);
		mv.visitJumpInsn(Instructions.genericComparisonOpcode(opcode), label);
	}

	/**
	 * Encodes an OR expression and its sub-expressions.
	 */
	protected void or(Expression left, Expression right, Label t, Label f)
	{
		if(t != null)
		{
			// branch on true
			logicalExpression(left, t, null);
			logicalExpression(right, t, null);
		}
		else
		{
			// branch on false
			t = new Label();
			logicalExpression(left, t, null);
			logicalExpression(right, null, f);
			mv.visitLabel(t);
		}
	}
	
	/**
	 * Encodes an AND expression and its sub-expressions.
	 * 
	 * @param left	left operand
	 * @param right right operand
	 * @param t   	where to jump if true
	 * @param f     where to jump if false
	 */
	protected void and(Expression left, Expression right, Label t, Label f)
	{
		if(t != null)
		{
			// branch on true
			f = new Label();
			logicalExpression(left, null, f);
			logicalExpression(right, t, null);
			mv.visitLabel(f);
		}
		else
		{
			// branch on false
			logicalExpression(left, null, f);
			logicalExpression(right, null, f);
		}
	}
	
	/**
	 * Evaluates a binary logical expression, such as AND, OR, a comparison
	 * or other logical expression (e.g. IN or MATCH). At least one of the 
	 * labels must be non-null.
	 * 
	 * @param exp
	 * @param t
	 * @param f
	 */
	protected void binaryLogicalExpression(BinaryExpression exp, Label t, Label f)
	{
		Operator op = exp.operator();
		Expression left = exp.left();
		Expression right = exp.right(); 
		
		if(op == Operator.OR)
		{
			or(left, right, t, f);
			return;
		}
		if(op == Operator.AND)
		{
			and(left, right, t, f);
			return;
		}
		int opcode = OPERATOR_TO_BYTECODE.get(op);
		if(opcode != 0)
		{
			comparison(opcode, left, right, t, f);
			return;
		}
		if(op == Operator.MATCH)
		{
			matchPattern(left, right, t, f);
			return;
		}
		// TODO: IN
		throw new RuntimeException(String.format(
			"Logical Operator %s is not implemented.", op.name()));
	}
	
	/**
	 * Emits code to evaluate a logical expression.
	 * If the expression is binary, both t and f  can be non-null. 
	 * Otherwise, only one of the labels may be specified.
	 * TODO: it should always be only one or the other!
	 * 
	 * @param exp	  the Expression
	 * @param t       the Label where to jump if the expression 
	 * 				  is true (if null, the code branches to 
	 * 				  f if the expression is false) 
	 * @param f       the Label where to jump if the expression 
	 * 				  is false (if null, the code branches to 
	 * 				  t if the expression is true) 
	 */
	protected void logicalExpression(Expression exp, Label t, Label f)
	{
		if(exp instanceof BinaryExpression)
		{
			binaryLogicalExpression((BinaryExpression)exp, t, f);
			return;
		}
		if(exp instanceof UnaryExpression)
		{
			UnaryExpression unary = (UnaryExpression)exp;
			Operator op = unary.operator();
			Expression operand = unary.operand();
			if (op == Operator.NOT)
			{
				logicalExpression(operand, f, t);
				return;
			}
			// TODO: evaluate, coerce to boolean
		}
	}

	private static ObjectIntMap<Operator> OPERATOR_TO_BYTECODE;
	
	static
	{
		ObjectIntHashMap<Operator> m = new ObjectIntHashMap<>();
		m.put(Operator.EQ, Opcodes.IF_ICMPEQ);
		m.put(Operator.NE, Opcodes.IF_ICMPNE);
		m.put(Operator.GE, Opcodes.IF_ICMPGE);
		m.put(Operator.GT, Opcodes.IF_ICMPGT);
		m.put(Operator.LE, Opcodes.IF_ICMPLE);
		m.put(Operator.LT, Opcodes.IF_ICMPLT);
		OPERATOR_TO_BYTECODE = m;
	}

	@Override public Void visitVariable(Variable exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Void visitCall(CallExpression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public Void visitConditional(ConditionalExpression exp)
	{
		// TODO: should store type in Expression
		Label f = new Label();
		Expression ifTrue = exp.ifTrue();
		Expression ifFalse = exp.ifFalse();
		Class<?> type = typeChecker.commonType(ifTrue, ifFalse);
		logicalExpression(exp.condition(), null, f);
		evaluate(exp.ifTrue(), type);
		mv.visitLabel(f);
		evaluate(exp.ifFalse(), type);
		return null;
	}

	@Override public Void visitExpression(Expression exp)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
