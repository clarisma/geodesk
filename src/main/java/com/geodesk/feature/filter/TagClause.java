package com.geodesk.feature.filter;

import com.clarisma.common.ast.BinaryExpression;
import com.clarisma.common.ast.Expression;
import com.clarisma.common.ast.Operator;
import com.clarisma.common.ast.Variable;

public class TagClause extends Variable implements Comparable<TagClause>
{
	private int flags;
	private int key;
	private int category;
	private Expression exp;
	public TagClause next;

	// explicit is [k], implicit are all others except [k!=v] and [k!~v]
	public static final int KEY_REQUIRED_EXPLICITLY = 128;
	public static final int KEY_REQUIRED_IMPLICITLY = 64;

	// The types of values that a clause considers

	public static final int VALUE_GLOBAL_STRING = 1;
	public static final int VALUE_LOCAL_STRING = 2;
	public static final int VALUE_ANY_STRING = 4;
	public static final int VALUE_DOUBLE = 8;
	public static final int VALUE_ANY = 15;

	public TagClause(int flags, String keyString, int key, int category, Expression exp)
	{
		super(keyString);
		this.flags = flags;
		this.key = key;
		this.category = category;
		this.exp = exp;
	}

	public int flags() { return flags; }
	
	public TagClause next()
	{
		return next;
	}
	
	public Expression expression()
	{
		return exp;
	}
	
	public void setExpression(Expression exp)
	{
		this.exp = exp;
	}
	
	public int keyCode()
	{
		return key;
	}

	public int category()
	{
		return category;
	}
	
	public boolean isKeyRequired()
	{
		return (flags & (KEY_REQUIRED_EXPLICITLY | KEY_REQUIRED_IMPLICITLY)) != 0;
	}

	public boolean isLocalKey() { return key == 0; }

	public boolean isGlobalKey() { return key != 0; }

	/**
	 * Tag Clauses are grouped and sorted as follows:
	 * - Common tags in ascending order of the key code 
	 * - Uncommon tags in ascending alphabetical order
	 */
	@Override public int compareTo(TagClause other)
	{
		if(key == 0)
		{
			return other.key == 0 ? name().compareTo(other.name()) : 1;
		}
		return other.key == 0 ? -1 : Integer.compare(key, other.key);
	}

	/**
	 * Checks if this clause can be AND-combined with another clause.
	 * If so, returns true. If the other clause is superfluous, changes
	 * both clauses to make sense and returns false, in which case no
	 * further merging steps should be taken.
	 * If a combination of clauses is nonsensical, throws QueryException.
	 *
	 * @param other
	 * @return true if the clauses can be combined, false if they are
	 * 	problematic but have been fixed
	 */
	private boolean checkConjoined(TagClause other)
	{
		if(!isKeyRequired() && exp==null)
		{
			if (other.isKeyRequired())
			{
				// [!k][k] and [!k][k=v] will never yield any results

				throw new QueryException(String.format(
					"Conflicting clauses for key %s", name()));
				// return false;
			}
			else if (other.exp != null)
			{
				// [!k] combined with [k!=v] is simply [!k]
				other.exp = null;
				return false;
			}
		}
		return true;
	}

	/**
	 * Merges another TagClause into this one. For example, a filter query
	 * may consist of two clauses with the same key, e.g. [k>3][k<8].
	 * The generated code expects to visit each tag only once, so we need
	 * to merge the two clauses into a single one that contains an AND
	 * expression.
	 *
	 * @param other another TagClause with the same key
	 * @param conjoin true if the expressions should be combined using
	 *      AND, or false if logical OR should be used.
	 */
	public void absorb(TagClause other, boolean conjoin)
	{
		if(conjoin)
		{
			if(!checkConjoined(other)) return;
			if(!other.checkConjoined(this)) return;
			flags |= other.flags;
			if(exp == null)
			{
				exp = other.exp;
			}
			else if(other.exp != null)
			{
				exp = new BinaryExpression(Operator.AND, exp, other.exp);
			}
		}
		else
		{
			// TODO
		}
	}

	/*
	protected void absorb(TagClause other, boolean conjoin)
	{
		if(flags != other.flags && (flags != 0 || other.flags != 0))
		{
			// TODO: can't mix KEY_MUST_BE_PRESENT and KEY_MUST_BE_ABSENT
		}
		flags |= other.flags;
		if(conjoin)
		{
			if(other.firstChild == null) return;
			if(firstChild == null)
			{
				firstChild = other.firstChild;
				return;
			}
			firstChild = ValueClause.and(firstChild.next == null ? firstChild :
				ValueClause.or(firstChild));
			// TODO
		}
		else
		{
			addChildrenOf(other, false);
		}
	}
	*/
}
