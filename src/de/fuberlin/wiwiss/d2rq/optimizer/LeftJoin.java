package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.List;
import java.util.Set;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;

/**
 * Contains all necessary data for an left-join on sql-layer
 *
 * @author Herwig Leimer
 *
 */
public class LeftJoin 
{
	private RelationName relation1;
	private RelationName relation2;
	private Expression joinConditions;
	
	public LeftJoin(List mentionedTableNames, Set joinConditions)
	{
		this.relation1 = (RelationName) mentionedTableNames.get(0);
		this.relation2 = (RelationName) mentionedTableNames.get(1);
		this.joinConditions = Conjunction.create(joinConditions);
	}

	public RelationName getRelation1() 
	{
		return relation1;
	}

	public RelationName getRelation2() 
	{
		return relation2;
	}

	public Expression getJoinConditions() 
	{
		return joinConditions;
	}

	public void swapRelationNames()
	{
		RelationName tmpRelation;
		
		tmpRelation = this.relation1;
		this.relation1 = this.relation2;
		this.relation2 = tmpRelation;
	
	}
	
	public boolean equals(Object obj) 
	{		
		boolean equal = false;
		LeftJoin leftJoin;
		
		if (obj instanceof LeftJoin)
		{
			leftJoin = (LeftJoin)obj;
			equal = leftJoin.joinConditions.equals(this.joinConditions) &&
					leftJoin.relation1.equals(this.relation1) &&
					leftJoin.relation2.equals(this.relation2);
		}
		
		return equal;
	}
	
	
}
