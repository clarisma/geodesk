package com.geodesk.feature.query;

import com.clarisma.common.ast.AstVisitor;
import com.clarisma.common.ast.Expression;

public class Selector extends Expression
{
	public static final int MATCH_NODES = 1; 
	public static final int MATCH_WAYS = 2; 
	public static final int MATCH_AREAS = 4; 
	public static final int MATCH_RELATIONS = 8;
	public static final int MATCH_ALL = 15;

	/**
	 * The types of clauses that are present in this Selector:
	 * local/global, required/optional. A required-key clause only matches if
	 * its key is present; an optional-key clause can match even if the key
	 * is missing (e.g. [!key], [key!=abc])
	 */
	public static final int CLAUSE_LOCAL_REQUIRED = 1;
	public static final int CLAUSE_LOCAL_OPTIONAL = 2;
	public static final int CLAUSE_GLOBAL_REQUIRED = 4;
	public static final int CLAUSE_GLOBAL_OPTIONAL = 8;

	private final int matchTypes;
	private int clauseTypes;
	private int indexBits;
	private Selector next;
	private TagClause firstClause;
	// TODO: role
	
	public Selector(int matchTypes)
	{
		this.matchTypes = matchTypes;
	}
	
	public int matchTypes()
	{
		return matchTypes;
	}

	public int clauseTypes()
	{
		return clauseTypes;
	}

	public int indexBits()
	{
		return indexBits;
	}

	public Selector next()
	{
		return next;
	}
	
	public void setNext(Selector sel)
	{
		next = sel;
	}
	
	TagClause firstClause()
	{
		return firstClause;
	}

	// TODO: deal with roles here
	public void add(TagClause clause)
	{
		if(clause.isKeyRequired())
		{
			clauseTypes |= clause.keyCode() == 0 ?
				CLAUSE_LOCAL_REQUIRED : CLAUSE_GLOBAL_REQUIRED;
			indexBits |= IndexBits.fromCategory(clause.category());
		}
		else
		{
			clauseTypes |= clause.keyCode() == 0 ?
				CLAUSE_LOCAL_OPTIONAL : CLAUSE_GLOBAL_OPTIONAL;
		}
		if(firstClause == null)
		{
			firstClause = clause;
			clause.next = null;
			return;
		}
		int comp = clause.compareTo(firstClause);
		if(comp == 0) 
		{
			firstClause.absorb(clause,  true);
			return;
		}
		if(comp < 0)
		{
			clause.next = firstClause;
			firstClause = clause;
			return;
		}
		TagClause prev = firstClause;
		for(;;)
		{
			TagClause c = prev.next;
			if(c == null)
			{
				prev.next = clause;
				clause.next = null;
				return;
			}
			comp = clause.compareTo(c);
			if(comp == 0) 
			{
				c.absorb(clause, true);
				return;
			}
			if(comp < 0)
			{
				prev.next = clause;
				clause.next = c;
				return;
			}
			prev = c;
		}
	}
	
	@Override public <R> R accept(AstVisitor<R> visitor)
	{
		visitor.visitExpression(this);
		return null;
	}
}
