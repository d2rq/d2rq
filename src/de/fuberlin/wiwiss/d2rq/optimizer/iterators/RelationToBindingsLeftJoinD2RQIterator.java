package de.fuberlin.wiwiss.d2rq.optimizer.iterators;

import java.util.Collection;
import java.util.Iterator;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.optimizer.VarFinder;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Iterator that calcualtes the result-bindings of an OpLeftJoinD2RQ.
 * Difference to a RelationToBindingsIteratorD2RQ is that optional bindings
 * are regarded
 *
 * @author Herwig Leimer
 *
 */
public class RelationToBindingsLeftJoinD2RQIterator extends RelationToBindingsD2RQIterator
{
	private VarFinder varFinder;
	
	/**
	 * Creates a new RelationToBindingsLeftJoinD2RQIterator
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 * @return QueryIterator - a QueryIterator
	 */
	public static QueryIterator create(Relation relation, Collection bindingMakers, QueryIterator input, ExecutionContext context, VarFinder varFinder) 
	{
		if (relation.condition().isFalse() || relation.isTrivial()) 
		{
			input.close();
			return new QueryIterNullIterator(context);
		}
		return new RelationToBindingsLeftJoinD2RQIterator(relation, bindingMakers, input, context, varFinder);
	}
	
	/**
	 * Constructor
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 */
	protected RelationToBindingsLeftJoinD2RQIterator(Relation relation, Collection bindingMakers, QueryIterator input, ExecutionContext context, VarFinder varFinder)
    {
		super(relation, bindingMakers, input, context);
		this.varFinder = varFinder;
	}

	/**
	 * override from super-class
	 */
	protected QueryIterator nextStage(Binding binding) 
	{	
		return new StagePattern(binding, relation, bindingMakers, getExecContext())
		{
			// override from superclass
			// difference is, that optional bindings are made -
			// method makeOptionalBinding from BindingMaker is called
			// OptionalBinding: if a binding has no value, it will not be rejected
			protected void enqueueBindings(ResultRow row) 
	    	{
	    		Iterator it = bindingMakers.iterator();
	    		while (it.hasNext()) 
	    		{
	    			BindingMaker bindingMaker = (BindingMaker) it.next();
	    			Binding binding = bindingMaker.makeOptionalBinding(row, varFinder);
	    			if (binding != null) 
	    			{
	    				queue.add(binding);
	    			}
	    		}
	    	}
		};
	}
	
}

