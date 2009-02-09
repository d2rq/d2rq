package de.fuberlin.wiwiss.d2rq.optimizer.ops;

import java.util.Collection;

import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.OpD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.VarFinder;
import de.fuberlin.wiwiss.d2rq.optimizer.iterators.RelationToBindingsLeftJoinD2RQIterator;

/**
 * Repesents a special D2RQ Operator, that performs a
 * left-join on sql-layer
 * @author Herwig Leimer
 *
 */
public class OpLeftJoinD2RQ extends OpD2RQ//extends OpExtMain 
{
	private VarFinder varFinder;
	
	/**
	 * Constructor
	 * @param original
	 * @param relation
	 * @param bindingMakers
	 */
	public OpLeftJoinD2RQ(OpBGP original, Relation relation, Collection bindingMakers, VarFinder varFinder) {
		super(original, relation, bindingMakers);
		this.varFinder = varFinder;
	}
	
	/**
	 * Iterator for getting the results of the leftjoin
	 */
	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) 
	{
		return RelationToBindingsLeftJoinD2RQIterator.create(relation, bindingMakers, input, execCxt, varFinder);
	}

	public String getSubTag() {
		return "leftjoind2rq";
	}
	
}
