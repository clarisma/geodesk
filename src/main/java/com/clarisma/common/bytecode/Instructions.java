/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.bytecode;

import static org.objectweb.asm.Opcodes.*;

public class Instructions
{
	/**
	 * For a given comparison opcode, returns its opposite.
	 * 
	 * TODO: Could also say:
	 * 	- If even, subtract one
	 *  - If odd, add one
	 *  Works for opcodes 153 to 166, but not for ifnull/ifnonnull (198/199)
 	 *
	 * @param op
	 * @return
	 */
	/*
	public static int oppositeComparisonOpcode(int op)
	{
		switch(op)
		{
		case IF_ICMPEQ: return IF_ICMPNE; 
		case IF_ICMPNE: return IF_ICMPEQ;
		case IF_ICMPLT: return IF_ICMPGE;
		case IF_ICMPLE: return IF_ICMPGT;
		case IF_ICMPGT: return IF_ICMPLE;
		case IF_ICMPGE: return IF_ICMPLT;
		case IFEQ: 		return IFNE; 
		case IFNE: 		return IFEQ;
		case IFLT: 		return IFGE;
		case IFLE: 		return IFGT;
		case IFGT: 		return IFLE;
		case IFGE: 		return IFLT;
		case IF_ACMPEQ: return IF_ACMPNE; 
		case IF_ACMPNE: return IF_ACMPEQ;
		case IFNULL: 	return IFNONNULL; 
		case IFNONNULL: return IFNULL;
		default:
			throw new IllegalArgumentException(
				String.format("%0X is not a comparison opcode", op));
		}
	}
	*/
	
	public static int negate(int op) 
	{
		// TODO: assert comparison operator
        if (op == IFNULL) return IFNONNULL;
        if (op == IFNONNULL) return IFNULL;
        return ((op + 1) ^ 1) - 1;
    }
	
	/**
	 * For a given integer comparison opcode, returns its generic 
	 * comparison opcode.
	 * 
	 * @param intCompareOp  the integer opcode (e.g. IF_ICMPGT)
	 * @return its equivalent generic form (e.g. IFGT)
	 */
	public static int genericComparisonOpcode(int intCompareOp)
	{
		assert intCompareOp >= IF_ICMPEQ && intCompareOp <= IF_ICMPLE;
		return intCompareOp - 6;
	}
	
	/**
	 * For a given integer arithmetic opcode, returns the type-specific 
	 * opcode. 
	 * 
	 * @param intOp    the arithmetic opcode for int (IADD, ISUB, IMUL,
	 * 				   IDIV, IREM, INEG)
	 * @param type     the primitive type for which to obtain the opcode
	 * @return the type-specific opcode (e.g. DADD, DSUB, etc.)
	 */
	public static int mathOpcodeForType(int intOp, Class<?> type)
	{
		if(type == Integer.TYPE) return intOp;
		if(type == Double.TYPE) return intOp+3;
		if(type == Long.TYPE) return intOp+1;
		if(type == Float.TYPE) return intOp+2;
		return intOp;
	}
}
