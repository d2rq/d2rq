/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.PatternStageCompiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Bind;
import com.hp.hpl.jena.graph.query.Bound;
import com.hp.hpl.jena.graph.query.BufferPipe;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Element;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Fixed;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.Stage;
import com.hp.hpl.jena.graph.query.Valuator;
import com.hp.hpl.jena.graph.query.ValuatorSet;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.helpers.VariableIndex;

/**
 * A CombinedPatternStage is a {@link Stage} that handles a conjunction of triples (a pattern). 
 * This class is a corrected, clarified and optimized version of {@link PatternStage}.
 * In the following we try to document the logic and machinery behind it:
 * <p>
 * An RDQL query (<code>triples</code>) contains nodes, some of which are
 * (shared) variables (a-z) which will be bound by successive Stages. 
 * This Stage for example sees variables a,b,c,i,j,k,x,y,z in <code>triples</code>.
 * The type of the triple nodes (fixed, bound or bind Element) is 
 * <code>compiled</code> according to the following schema:
 * <p>
 * The mapping <code>map</code> lists variables a,b,c...h (pointers to Domain indices)
 * that will be bound by a previous stage and put into the Pipe. ({@link Bound})
 * This stage will pick up each binding (on its own thread), 
 * substitute variables with existing bindings, and produce additional bindings.
 * This stage is first to bind some variables i,j,k,x,y,z (1st time {@link Bind})
 * Some variables x,y,z are used in more than one triple/node => (2nd time: {@link Bound}) 
 * <p>
 * Some variables l,m,n...u,v,w will still not be bound by this stage and left over 
 * for the next stage.
 * 
 * compiled: for each query triple the compiled node binding information ({@link Bind}, {@link Bound}, {@link Fixed})
 * guard: a condition checker for the conditions that come with the query and can
 * be checked after this stage found a matching solution for the variables, 
 * for example (?x < ?y)
 * varInfo: fast lookup information for different types of variables (shared, bind, bound)
 * triples: list of find-triples with inserted bindings by previous stages, 
 * build each time a binding arrives on the {@link Pipe}.
 * @author Joerg Garbers
 * @since V0.3
 */
public abstract class CombinedPatternStage4 extends Stage {
	
	StageInfo stageInfo;
	protected boolean mayYieldResults=true;
	
	// used in run()
	/** A list of find-triples with inserted bindings by previous stages, 
	 * build each time a binding arrives on the {@link Pipe}.
	 */
	protected Triple[] triples;
	
	public CombinedPatternStage4(Graph g, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		stageInfo=new StageInfo(map,constraints,triples);
		this.triples=new Triple[triples.length];
	}

	public abstract void setup();

    /**
     * Realizes the piping between the previous, this and the following {@link Stage}.
     * A new thread is created for this stage.
     * @see PatternStage
     * @see #run(Pipe, Pipe)
     */
 	public Pipe deliver(final Pipe result) {
 		if (!mayYieldResults) {
 			result.close();
 			return result;
 		}
		final Pipe stream = previous.deliver(new BufferPipe());
		new Thread() {
			public void run() {
				CombinedPatternStage4.this.run(stream, result);
			}
		}.start();
		return result;
	}
    
	/**
 	 * Pulls variable bindings from the previous stage adds bindings and pushes all
 	 * into the following stage.
 	 * A more efficient version of {@link PatternStage#run(com.hp.hpl.jena.graph.query.Pipe, com.hp.hpl.jena.graph.query.Pipe)}.
 	 * Includes its run(Pipe,Pipe) and nest() functionality.
 	 * Handles the case, where bindings for <code>triples</code> are not found triple by triple,
 	 * but in one step. This is the case for D2RQ.
 	 * <p>
 	 * In our example:
 	 *  a,b,c get bound by source. They are substituted by asTripleMatch(). (Bound)
	 *  i,j,k,x,y,z  are bound by p.match() the first time they are seen. (Bind)
	 *  x,y,z are checked by p.match() the second time they are seen. (Bound)
 	 * 
 	 * @param source the connector to the previous stage
 	 * @param sink the connector to the next stage
 	 */
	protected void run(Pipe source, Pipe sink) {
		while (stillOpen && source.hasNext()) {
			Domain inputDomain = source.get();
			stageInfo.vars.setInputDomain(inputDomain);
			findFrom(inputDomain,0,sink,null);
		}
		sink.close();
	}
	
//    protected void nest( Pipe sink, Domain current, int index )
//    {
//    if (index == compiled.length)
//        sink.put( current.copy() );
//    else
//        {
//        Pattern p = compiled[index];
//        ValuatorSet guard = guards[index];
//        ClosableIterator it = graph.find( p.asTripleMatch( current ) );
//        while (stillOpen && it.hasNext())
//            if (p.match( current, (Triple) it.next()) && guard.evalBool( current )) 
//                nest( sink, current, index + 1 );
//        it.close();
//        }
//    }
//}

	
	/**
	 * recursively calls findFrom until the triples.length is reached.
	 * @param nextToMatch
	 * @return the index nextToMatch
	 */
	protected void findFrom(Domain domain, int nextToFind, Pipe sink, Object aboutPrevious) {
		// the following two methods should be called in sync
		// because triples is often overridden
		updateTriplesWithDomain(domain, nextToFind);
		// get solutions from hook method and put in sink
		ClosableIterator it = resultIteratorForTriplePattern(triples,nextToFind, aboutPrevious);
		if (it==null)
			return;
		while (stillOpen && it.hasNext()) {
			IteratorResult res=(IteratorResult)it.next();
			Object aboutNext=res.about; 
			Triple[] resultTriples = res.triples;
			int resultLen=resultTriples.length;
			if (resultTriples==null || resultLen==0)
			    throw new RuntimeException("Iterator returns resultTriple array of length 0.");
			int next2=nextToFind+resultLen;
			boolean possible = true;
			// TODO check indices
			Domain copy=domain.copy();
			possible=matchAndEvalGuards(copy,resultTriples,nextToFind,next2-1);
			if (possible) {
				if (next2==triples.length)
					sink.put(copy);
				else
					findFrom(copy,next2,sink,aboutNext);
			}
		}
		it.close();
	}
	
	/**
	 * Matches domain with resultTriples.
	 * Thereby modifies domain.
	 * @param domain
	 * @param resultTriples
	 * @param from
	 * @param to
	 * @return
	 */
	protected boolean matchAndEvalGuards(Domain domain, Triple[] resultTriples, int from, int to) {
		boolean possible = true;
		//  evaluate all matches and guards at once
		int resultTripleIndex=0;
		for (int index = from; possible && (index <= to); index++, resultTripleIndex++) {
			Pattern p = stageInfo.compiled[index];
			possible = p.match(domain, resultTriples[resultTripleIndex]);
			if (possible && stageInfo.guards!=null) {
				stageInfo.guards[index].evalBool(domain);
			}
		}
		if (possible && stageInfo.guard!=null && to>=triples.length-1) {
			possible=stageInfo.guard.evalBool(domain);
		}
		return possible;
	}
	
	/**
	 * Updates <code>triples</code> with a new set of bindings from the previous stage.
	 * @param inputDomain for each variable number a binding entry
	 */
	public void updateTriplesWithDomain(Domain inputDomain, int nextToFind) {
	    // TODO can be more efficient, just check varInfo.boundDomainIndexToShared
		int tripleCount = stageInfo.compiled.length;
		// Triple[] triples = new Triple[tripleCount];
		for (int index = nextToFind; index < tripleCount; index++) {
			Pattern p = stageInfo.compiled[index];
			triples[index] = p.asTripleMatch(inputDomain).asTriple();
		}
	}
		
	/**
	 * It is the subclass' duty to create an iterator.
	 * Null means: empty iterator.
	 * @param triples all {@link Triple} nodes are fixed or ANY. 
	 * @return iterator that returns instanciations (<code>Triple[]</code>) of the
	 * <code>triples</code> find pattern.
	 */
	abstract ClosableIterator resultIteratorForTriplePattern(Triple[] triples, int nextToFind, Object aboutPrevious);
	
	public static class IteratorResult {
		public Triple[] triples;
		public Object about;
		public IteratorResult(Triple[] triples, Object about) {
			this.triples=triples;
			this.about=about;
		}
	}
}
