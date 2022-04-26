package com.clarisma.common.ast;

public class Operator
{
	private final String name;
	private final String symbol;
	private final float precedence;
	
	// TODO: associativity
	
	public Operator(String name, String symbol, float precedence)
	{
		this.name = name;
		this.symbol = symbol;
		this.precedence = precedence;
	}

	public String name()
	{
		return name;
	}

	public String symbol()
	{
		return symbol;
	}
	
	public float precedence()
	{
		return precedence;
	}

	@Override public int hashCode()
	{
		return symbol.hashCode();
	}

	// TODO: operator should always be treated as singleton
	@Override public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj instanceof Operator other)
		{
			return name.equals(other.name) &&
				symbol.equals(other.symbol);
		}
		return false;
	}

	public String toString()
	{
		return symbol;
	}
	
	public static final float COMPARISON_LEVEL = 40;
	public static final float ADDITION_LEVEL = 50;
	public static final float MULTIPLICATION_LEVEL = 60;
	
	// Arithmetic
	
	public static final Operator ADD = new Operator("add", "+", ADDITION_LEVEL);
	public static final Operator SUBTRACT = new Operator("sub", "-", ADDITION_LEVEL);
	public static final Operator MULTIPLY = new Operator("mul", "*", MULTIPLICATION_LEVEL);
	public static final Operator DIVIDE = new Operator("div", "/", MULTIPLICATION_LEVEL);
	public static final Operator MODULO = new Operator("mod", "%", MULTIPLICATION_LEVEL);
	public static final Operator UNARY_MINUS = new Operator("neg", "-", MULTIPLICATION_LEVEL+5);
	
	// Comparison
	
	public static final Operator EQ = new Operator("eq", "=", COMPARISON_LEVEL);
	public static final Operator NE = new Operator("ne", "!=", COMPARISON_LEVEL);
	public static final Operator LT = new Operator("lt", "<", COMPARISON_LEVEL);
	public static final Operator LE = new Operator("le", "<=", COMPARISON_LEVEL);
	public static final Operator GT = new Operator("gt", ">", COMPARISON_LEVEL);
	public static final Operator GE = new Operator("ge", ">=", COMPARISON_LEVEL);
	
	public static final Operator IN = new Operator("in", "in", COMPARISON_LEVEL);
	public static final Operator NOT_IN = new Operator("notIn", null, COMPARISON_LEVEL);
	public static final Operator MATCH = new Operator("match", "like", COMPARISON_LEVEL);
	public static final Operator NOT_MATCH = new Operator("notMatch", null, COMPARISON_LEVEL);
		// TODO: needed?

	// Logical
	
	public static final Operator AND = new Operator("and", "and", COMPARISON_LEVEL-10);
	public static final Operator OR = new Operator("or", "or", COMPARISON_LEVEL-20);
	public static final Operator NOT = new Operator("not", "not", COMPARISON_LEVEL-5);
}
