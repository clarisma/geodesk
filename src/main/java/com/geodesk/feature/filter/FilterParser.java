package com.geodesk.feature.filter;

import com.clarisma.common.ast.*;
import com.clarisma.common.math.MathUtils;
import com.geodesk.feature.store.TagValues;
import com.clarisma.common.parser.Parser;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

import java.util.regex.Pattern;

// TODO: Parser is not threadsafe!

// TODO: It is possible (though rare) that a wildcard string is turned into a CommonString,
//  in which case it is not turned into a startsWith/endsWith expression

// TODO: When parsing value of a role clause, be aware that the range of valid
//  global-string roles is different from keys

// TODO: consider ambiguities if we implement simplified form
//  The type letter may appear in any order, could they represent valid keys?
//  "war", "raw", "ran", "warn"?
//  What if we allow other letter for types?
//  l = line, c = collection?

public class FilterParser extends Parser
{
	private static final String COMMA = ",";
	private static final String STAR = "*";
	private static final String COLON = ":";
	private static final String EXCLAMATION_MARK = "!";
	private static final String LBRACKET = "[";
	private static final String RBRACKET = "]";

	private final static Pattern KEY_IDENTIFIER_PATTERN =
		Pattern.compile("[a-zA-Z_][\\w:]*");
	// TODO: make pattern stricter?
	//  No, should make more lenient:
	//    "[a-zA-Z_\u00C0-\u1FFF\u2C00-\uD7FF][\\w:\u00C0-\u1FFF\u2C00-\uD7FF]*"
	// TODO: move these?
	// TODO: should we implement these as functions instead?
	public static final Operator STARTS_WITH =
		new Operator("startsWith", null, Operator.COMPARISON_LEVEL);
	public static final Operator ENDS_WITH =
		new Operator("endsWith", null, Operator.COMPARISON_LEVEL);

	private static final int OP_REQUIRES_KEY = 1;
	private static final int OP_NUMERIC = 2;
	private static final int OP_STRING = 4;
	private static final int OP_LIST = 8;
	private static final int OP_EQUAL = 16;
	private static final int OP_EXACT = 32;

	private final ObjectIntMap<String> stringsToCodes;
	private final IntIntMap keysToCategories;

	public FilterParser(ObjectIntMap<String> stringsToCodes, IntIntMap keysToCategories)
	{
		if (stringsToCodes == null) stringsToCodes = new ObjectIntHashMap<>();
		this.stringsToCodes = stringsToCodes;
		if (keysToCategories == null) keysToCategories = new IntIntHashMap();
		this.keysToCategories = keysToCategories;
		addTokens(COMMA, STAR, COLON, EXCLAMATION_MARK, LBRACKET, RBRACKET,
			Operator.EQ, Operator.NE, Operator.GT, Operator.GE, Operator.LT,
			Operator.LE);
		addToken("~", Operator.MATCH);
		addToken("!~", Operator.NOT_MATCH);
	}

	/**
	 * Matches an identifier string and returns a bit field with the bits
	 * representing the types accepted by the current selector.
	 *
	 * @return  type mask, or 0 if the type specifier is not valid
	 */
	private int featureTypes()
	{
		int types = 0;
		expect(IDENTIFIER);
		String s = stringValue();
		nextToken();		// TODO: don't advance to next token if we allow
							//  simplified form (in which case the current
							//  identifier may represent a key
		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);
			switch (ch)
			{
			case 'n':
				if((types & TypeBits.NODES) == TypeBits.NODES) return 0;
				types |= TypeBits.NODES;
				break;
			case 'w':
				if((types & TypeBits.NONAREA_WAYS) == TypeBits.NONAREA_WAYS) return 0;
				types |= TypeBits.NONAREA_WAYS;
				break;
			case 'a':
				if((types & TypeBits.AREAS) == TypeBits.AREAS) return 0;
				types |= TypeBits.AREAS;
				break;
			case 'r':
				if((types & TypeBits.NONAREA_RELATIONS) == TypeBits.NONAREA_RELATIONS) return 0;
				types |= TypeBits.NONAREA_RELATIONS;
				break;
			default:
				error(String.format("Unknown feature type '%c', should be 'n','w','a', or 'r'", ch));
				return 0;
			}
		}
		return types;
	}

	private int keyCode(String key)
	{
		int keyCode = stringsToCodes.get(key);
		return keyCode <= TagValues.MAX_COMMON_KEY ? keyCode : 0;
	}

	private int stringCode(String key)
	{
		return stringsToCodes.get(key);
	}

	/*
	private String key()
	{
		String key;
		if(accept(IDENTIFIER))
		{
			key = stringValue();
			nextToken();
			if(accept(COLON))
			{
				StringBuilder buf = new StringBuilder();
				buf.append(key);
				while(accept(COLON))
				{
					buf.append(':');
					nextToken();
					expect(IDENTIFIER);
					buf.append(stringValue());
					nextToken();
				}
				return buf.toString();
			}
		}
		else
		{
			if(!accept(STRING)) return null;
			key = unquotedStringValue();
			nextToken();
		}
		return key;
	}
	*/

	private String key()
	{
		String key;
		if (accept(IDENTIFIER))
		{
			key = stringValue();
		}
		else
		{
			if (!accept(STRING)) return null;
			key = unquotedStringValue();
		}
		nextToken();
		return key;
	}

	private String expectKey()
	{
		String key = key();
		if (key != null) return key;
		errorExpected("key");
		return null;
	}

	private Operator operator()
	{
		if (tokenType instanceof Operator)
		{
			Operator op = (Operator) tokenType;
			nextToken();
			return op;
		}
		return null;
	}

	/*
	private Double numberValue()
	{
		if (accept(NUMBER))
		{
			Double v = doubleValue();
			nextToken();
			return v;
		}
		return null;
	}
	*/

	private Object comparisonValue(int opFlags)
	{
		Object val;
		int type;
		if (accept(NUMBER))
		{
			val = doubleValue();
			type = OP_NUMERIC;
		}
		else if (accept(IDENTIFIER))
		{
			val = stringValue();
			type = OP_STRING;
		}
		else if (accept(STRING))
		{
			val = unquotedStringValue();
			type = OP_STRING;
		}
		else
		{
			type = 0;
			val = null;
		}
		if((opFlags & type) == 0)
		{
			int typeFlags = opFlags & (OP_NUMERIC | OP_STRING);
			switch(typeFlags)
			{
			case OP_NUMERIC:
				errorExpected("number");
				return null;
			case OP_STRING:
				errorExpected("string");
				return null;
			default:
				errorExpected("string or number");
				return null;
			}
		}
		nextToken();
		return val;
	}

	private void errorExpected(String what)
	{
		error(String.format("Expected %s instead of %s", what, tokenValue));
	}

	private int operatorFlags(Operator op)
	{
		if(op == Operator.EQ)
		{
			return OP_REQUIRES_KEY | OP_NUMERIC | OP_STRING | OP_LIST |
				OP_EQUAL | OP_EXACT;
		}
		if(op == Operator.NE)
		{
			return OP_NUMERIC | OP_STRING | OP_LIST | OP_EXACT;
		}
		if(op == Operator.MATCH)
		{
			return OP_REQUIRES_KEY | OP_STRING | OP_LIST | OP_EQUAL;
		}
		if(op == Operator.NOT_MATCH)
		{
			return OP_STRING | OP_LIST;
		}
		return OP_REQUIRES_KEY | OP_NUMERIC;
	}

	private static boolean isNumericString(String s)
	{
		int len = s.length();
		if(len == 0) return false;
		char ch = s.charAt(0);
		if((ch < '0' || ch > '9') && ch != '-') return false;
		return MathUtils.countNumberChars(s) == len;
	}

	// TODO: decide on a common exit point where we reset the identifier pattern
	private TagClause tagClause()
	{
		if (!accept(LBRACKET)) return null;
		// change the identifier pattern to support colons in keys
		// without requiring the key to be put in quotes
		setIdentifierPattern(KEY_IDENTIFIER_PATTERN);
		nextToken();

		String key;
		int flags = 0;
		Expression exp = null;

		if (acceptAndConsume(EXCLAMATION_MARK))
		{
			key = expectKey();
			if (key == null) return null;
			flags = TagClause.VALUE_GLOBAL_STRING;
				// [!k] requires global key because we need to check for "no"
		}
		else
		{
			key = expectKey();
			if (key == null) return null;
			Operator op = operator();
			if (op == null)
			{
				flags = TagClause.KEY_REQUIRED_EXPLICITLY |
					TagClause.VALUE_GLOBAL_STRING;
				// [k] requires global key because we need to check for "no"
			}
			else
			{
				int opFlags = operatorFlags(op);
				if ((opFlags & OP_REQUIRES_KEY) != 0)
				{
					flags |= TagClause.KEY_REQUIRED_IMPLICITLY;
				}
				// TODO: convert multi-value comparisons into IN / NOT_IN
				// (only for common strings -- and numbers?)
				for (; ; )
				{
					Expression term;
					boolean negate = false;
					Object val = comparisonValue(opFlags);
					if(val == null) return null;

					Operator effectiveOp = op;
					if (val instanceof Double)
					{
						flags |= TagClause.VALUE_DOUBLE;
					}
					else
					{
						String s = (String)val;
						if ((opFlags & OP_EXACT) != 0)
						{
							int len = s.length();
							if (len > 0)
							{
								if (s.charAt(0) == '*')
								{
									negate = (opFlags & OP_EQUAL) == 0;
									if (s.charAt(len - 1) == '*')
									{
										effectiveOp = Operator.IN;
										val = len==1 ? "" : s.substring(1, len-1);
									}
									else
									{
										effectiveOp = ENDS_WITH;
										val = s.substring(1);
									}
									flags |= TagClause.VALUE_LOCAL_STRING |
										TagClause.VALUE_ANY_STRING;
								}
								else if (s.charAt(len - 1) == '*')
								{
									negate = (opFlags & OP_EQUAL) == 0;
									effectiveOp = STARTS_WITH;
									val = s.substring(0, len - 1);
									flags |= TagClause.VALUE_LOCAL_STRING |
										TagClause.VALUE_ANY_STRING;
								}
							}
						}
						// if((opFlags & OP_EXACT) != 0)
						if(effectiveOp == Operator.EQ || effectiveOp == Operator.NE)
						{
							// TODO: when accepting values for "role", be aware
							//  that not all global strings can be used as role values
							int code = stringCode(s);
							if (code == 0)
							{
								val = s;
								flags |= TagClause.VALUE_LOCAL_STRING;
								if(isNumericString(s))
								{
									// If the string we're trying to match is a number,
									// request that numeric tag values are converted to
									// a string
									// (We don't have to do this if the string is in the
									// global string table -- in this case, a tag value
									// will never be encoded as a number, and we can simply
									// do the cheap & fast global-string test)

									flags |= TagClause.VALUE_ANY_STRING;
								}
							}
							else
							{
								val = new GlobalString(s, code);
								flags |= TagClause.VALUE_GLOBAL_STRING;
							}
						}
					}
					term = new BinaryExpression(effectiveOp, new Variable(key), new Literal(val));
					if (negate) term = new UnaryExpression(Operator.NOT, term);
					exp = exp == null ? term : new BinaryExpression(
						(opFlags & OP_EQUAL) == 0 ? Operator.AND : Operator.OR, exp, term);
					if (!acceptAndConsume(COMMA)) break;
					if((opFlags & OP_LIST) == 0)
					{
						error(String.format(
							"Multiple values are not allowed for %s", op));
						return null;
					}
				}
			}
		}
		expect(RBRACKET);
		setIdentifierPattern(DEFAULT_IDENTIFIER_PATTERN);
		nextToken();
		int keyCode = keyCode(key);
		int category = keysToCategories.get(keyCode);
		return new TagClause(flags, key, keyCode, category, exp);
	}

	private Selector selector()
	{
		int types;
		if (acceptAndConsume(STAR))
		{
			types = TypeBits.ALL;
		}
		else
		{
			types = featureTypes();
			if (types == 0) return null;
		}
		Selector sel = new Selector(types);
		for (; ; )
		{
			TagClause clause = tagClause();
			if (clause == null) break;
			sel.add(clause);
		}
		return sel;
	}

	public Selector query()
	{
		Selector first = null;
		Selector prev = null;
		for (; ; )
		{
			Selector sel = selector();
			if (sel == null) break;
			if (prev == null)
			{
				first = sel;
			}
			else
			{
				prev.setNext(sel);
			}
			prev = sel;
			if (!acceptAndConsume(COMMA)) break;
		}
		expect(END);
		return first;
	}

	@Override public void parse(CharSequence s)
	{
		setIdentifierPattern(DEFAULT_IDENTIFIER_PATTERN);
		super.parse(s);
	}

	@Override protected void error(String msg)
	{
		msg = String.format("[%d:%d]: %s", line, column, msg);
		throw new QueryException(msg);
	}
}