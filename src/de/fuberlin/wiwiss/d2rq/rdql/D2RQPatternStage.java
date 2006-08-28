/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.PatternStageCompiler;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.graph.query.ValuatorSet;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;


/** 
 * Version of PatternStage for D2RQ that directly relies on {@link PatternStage}.
 * Created by Joerg Garbers on 25.02.05.
 * 
 * @author jgarbers
 * @see D2RQPatternStage2
 */
public class D2RQPatternStage extends PatternStage { // jg: reference PatternStage and QueryCombiner

	private GraphD2RQ graph;
	
	// This used to be in Jena's PatternStage, but was removed in 2.3,
	// so I duplicate it here [RC]
	private Pattern[] compiled;
	
	// instanciate just one PatternQueryCombiner? it could do some caching
	// or leave the caching for graph? e.g. triple -> list of bridges

	public D2RQPatternStage(GraphD2RQ graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		super((Graph) graph, map, constraints, triples);
		this.compiled = PatternStageCompiler.compile(new PatternStageCompiler(), map, triples);
		// some contraints are eaten up at this point!
		// so use s.th. like a clone() and setter method at invocation time
		D2RQPatternStage.this.graph = graph;
	}

/**
 * overridden from PatternStage (includes elements from run() and nest())
 */	
protected void run( Pipe source, Pipe sink ) {
	while (stillOpen && source.hasNext()) {
		Domain inputDomain=source.get();
		int tripleCount=compiled.length;
		Triple[] triples=new Triple[tripleCount];
		for (int index=0; index<tripleCount; index++) {
			Pattern p = compiled[index];
			triples[index] = p.asTripleMatch( inputDomain ).asTriple();
		}
		PatternQueryCombiner combiner=new PatternQueryCombiner(graph, null ,null,triples); // pass map?  constraints
		combiner.setup(); 
		// get solutins and put in sink
		// maybe it would be more efficient to reduce matching by checking just the difference set 
		// between the resultTriples and the previously successfully matched resultTriples
		ClosableIterator it = combiner.resultTriplesIterator();
		while (stillOpen && it.hasNext()) {
			Triple[] resultTriples=(Triple[]) it.next();
			if (resultTriples.length != tripleCount)
				throw new RuntimeException("D2RQPatternStage: PatternQueryCombiner returned triple array with wrong length");
			Domain current=inputDomain.copy();
			boolean possible=true;
			// maybe this can be more efficient: evaluate all matches and guards at once 
			for (int index=0; possible && (index<tripleCount); index++) {
				Pattern p = compiled[index];
				ValuatorSet guard = guards[index];
				possible = p.match( current, resultTriples[index]) && guard.evalBool( current );
			}
			if (possible) {
				sink.put(current.copy());
			}
		}
		it.close();		
	}
	sink.close();
}
} // class
