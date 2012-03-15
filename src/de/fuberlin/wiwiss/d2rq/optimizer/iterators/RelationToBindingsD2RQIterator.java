package de.fuberlin.wiwiss.d2rq.optimizer.iterators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.openjena.atlas.io.IndentedWriter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.Utils;

import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;
 
/**
 * Iterator that calculates the result-bindings of an OpD2RQ.
 *
 * @author Herwig Leimer
 *
 */
public class RelationToBindingsD2RQIterator extends QueryIterRepeatApply
{
	protected final Collection<BindingMaker> bindingMakers;
	protected final Relation relation;
	
	/**
	 * Creates a new RelationToBindingsD2RQIterator
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 * @return QueryIterator - a QueryIterator
	 */
	public static QueryIterator create(Relation relation, Collection<BindingMaker> bindingMakers, QueryIterator input, ExecutionContext context) 
	{
		if (relation.condition().isFalse() || relation.isTrivial()) 
		{
			input.close();
			return new QueryIterNullIterator(context);
		}
		return new RelationToBindingsD2RQIterator(relation, bindingMakers, input, context);
	}
	
	/**
	 * Constructor
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 */
	protected RelationToBindingsD2RQIterator(Relation relation, Collection<BindingMaker> bindingMakers, QueryIterator input, ExecutionContext context)
    {
		super(input, context) ;
    	this.bindingMakers = bindingMakers;
    	this.relation = relation;
	}
	
	/**
	 * Method for printing
	 */
	public void output(IndentedWriter out, SerializationContext cxt) 
	{
		out.print(Utils.className(this));
	}

	
	protected QueryIterator nextStage(Binding binding) 
	{	
		return new StagePattern(binding, relation, bindingMakers, getExecContext()) ;
	}
	
	/**
	 * Class that fetches additional values for every parent-binding.
	 * Especially necessary for Joins and LeftJoins.
	 * 
	 * @author Herwig Leimer
	 *
	 */
	static class StagePattern extends QueryIter
    {
        Binding parentBinding ;
        QueryExecutionIterator wrapped;
        LinkedList<Binding> queue = new LinkedList<Binding>();
        Collection<BindingMaker> bindingMakers;
        NodeMaker nodeMaker;
        ValueMaker valueMaker;
        Set<String> vars = new HashSet<String>();
              
        /**
         * Construtor
         * @param parentBinding - Parentbinding - for example join/leftjoin - the parentbinding will be a resultbinding from the left-OpD2RQ
         * @param relation - relation for getting the needed data from the database
         * @param bindingMakers - bindingMakers for creating the bindings
         * @param qCxt - execution-context
         */
        public StagePattern(Binding parentBinding, Relation relation, Collection<BindingMaker> bindingMakers, ExecutionContext qCxt)
        {
            super(qCxt) ;
            this.parentBinding = parentBinding ;
            this.bindingMakers = bindingMakers;
            Var var;
            MutableRelation mutableRelation = null;
            Expression expression;
            TypedNodeMaker typedNodeMaker;
            Node value;
            String valueAsString;
            
            // bindingmakers contain all information for making the bindings of the current relation
            for (BindingMaker bindingMaker: bindingMakers) {
            	// collect all vars of all bindingmakers
            	vars.addAll(bindingMaker.variableNames());
            	
            	// now check if every result(var) of the parentbinding is part of the
            	// the current relation and can be put into the current relation
            	for(Iterator<Var> bindingVars = parentBinding.vars(); bindingVars.hasNext();)
            	{
            		// get first binding-var
            		var = bindingVars.next();
            		
            		// is the binding var also used in this relation ?
            		if (bindingMaker.variableNames().contains(var.getName()))
            		{
            			// put the value of the binding-var to the relation
            			mutableRelation = new MutableRelation(relation);
            			nodeMaker = bindingMaker.nodeMaker(var.getName());
            			// TODO: type-cast perhaps a problem ??
            			typedNodeMaker = (TypedNodeMaker)nodeMaker;
            			valueMaker = typedNodeMaker.valueMaker();
            			
            			value = parentBinding.get(var);
                        
            			// TODO: some problems with other datatypes - eg. dates ??
            			if (value.isLiteral())
            	    	{
//            				valueAsString = value.getLiteralValue().toString();	
            				valueAsString = value.getLiteralValue().toString();
//            				valueAsString = ((Literal)value).getValue().toString();
            	    	}else
            	    	{
            	    		valueAsString = value.getURI();
            	    	}
            			
            			// create an equality-expression
            			expression = valueMaker.valueExpression(valueAsString);
            			// put the expression to the relation
            			relation = mutableRelation.select(expression); 
            		}
            	}
            }
            
            
            if (!relation.equals(Relation.EMPTY))
            {
            	SelectStatementBuilder sql = new SelectStatementBuilder(relation);
            	wrapped = new QueryExecutionIterator(sql.getSQLStatement(), sql.getColumnSpecs(), relation.database());
            }
        }

        /**
         * Check for next-binding
         */
    	public boolean hasNextBinding() 
    	{
    		
    		while (queue.isEmpty() && wrapped != null && wrapped.hasNext()) 
    		{
    			enqueueBindings(wrapped.next());
    		}
    		
    		return !queue.isEmpty();
    	}

    	/**
    	 * Closes the iterator
    	 */
        protected void closeIterator()
        {
        	if (wrapped != null)
        		wrapped.close();
        }

        /**
         * Go to the next binding
         */
    	public Binding moveToNextBinding() 
    	{
    		Binding b =  (Binding) queue.removeFirst();
    		BindingMap binding = new BindingHashMap(this.parentBinding) ;
    		
    		
			for (String name: vars) {
    			Var var = Var.alloc(name);
    			Node n = (Node)b.get(var) ;
                if ( n == null )
                    // There was no variable of this name.
                    continue ;
                if (!binding.contains(var))
                {
                	binding.add(var, n) ;
                }
            }
    		
    		return binding;
    		
    	}
    
    	/**
    	 * Makes the binding from a result-row of the database
    	 * and puts it into a queue
    	 * @param row
    	 */
    	protected void enqueueBindings(ResultRow row) 
    	{
			for (BindingMaker bindingMaker: bindingMakers) {
    			Binding binding = bindingMaker.makeBinding(row);
    			if (binding != null) 
    			{
    				queue.add(binding);
    			}
    		}
    	}
        
        @Override
        protected void requestCancel() { }
    }
	
	
}
