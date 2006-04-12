/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;


import java.util.ArrayList;
import java.util.Collection;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;


/**
 * Instances of this {@link com.hp.hpl.jena.graph.query.Stage} are created by {@link D2RQQueryHandler} to handle 
 * a set of query triples that by D2RQ-mapping refer to the same database.
 * Created by Joerg Garbers on 25.02.05.
 * 
 * @author jg
 * @since V0.3
 * @see CombinedPatternStage
 * @see PatternQueryCombiner
 */
public class D2RQPatternStage2 extends CombinedPatternStage {
    // TODO keep just one instance of PatternQueryCombiner and update Property Bridges
    // only when updated with previous stage (see varInfo.boundDomainIndexToShared)

	private GraphD2RQ graph;
	private Collection expressionSet=new ArrayList();

	// instanciate just one PatternQueryCombiner? it could do some caching
	// or leave the caching for graph? e.g. triple -> list of bridges

	public D2RQPatternStage2(GraphD2RQ graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		super((Graph) graph, map, constraints, triples);
		// some contraints are eaten up at this point!
		// so use s.th. like a clone() and setter method at invocation time
		D2RQPatternStage2.this.graph = graph;
	}
	
	// overridden
	protected void hookPrepareExpression(Expression e, boolean evaluable) {
	    if (evaluable)
	        expressionSet.add(e);
	}

	/**
	 * Sets up {@link PatternQueryCombiner} and returns its resultTriplesIterator.
	 * Passes stage information to the PatternQueryCombiner.
	 * @param triples 
	 * @return an iterator. Each result is a possible and full instanciation of
	 * triples according to D2RQ. 
	 */
	protected ClosableIterator resultIteratorForTriplePattern(Triple[] triples) {
		PatternQueryCombiner combiner = new PatternQueryCombiner(graph,
				varInfo, expressionSet, triples, relationships, compiled );
		combiner.setup();
		// maybe it would be more efficient to reduce matching by checking
		// just the difference set
		// between the resultTriples and the previously successfully matched
		// resultTriples
		ClosableIterator it = combiner.resultTriplesIterator();
		return it;
	}
}
