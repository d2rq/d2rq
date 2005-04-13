/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.PatternStageCompiler;
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
public abstract class CombinedPatternStage extends Stage {
	/** For each query triple the compiled node binding information ({@link Bind}, {@link Bound}, {@link Fixed}). */
	protected Pattern[] compiled; // triples with binding information
	/** A condition checker for the conditions that come with the query. They can
	 * be checked after this stage found a matching solution for the variables, 
	 * for example (?x < ?y).
	 */
	protected ValuatorSet guard; // a guard that checks the evaluable expressions from conditions
	/** Fast lookup information for different types of variables (shared, bind, bound). */
    protected VariableBindings varInfo=new VariableBindings();

    // used in compile()
	private int tripleNr, nodeNr;
	private Node tripleNodes[]=new Node[3]; // helper variable used for iterations
	private Element patternElements[]=new Element[3];

	// used in run()
	/** A list of find-triples with inserted bindings by previous stages, 
	 * build each time a binding arrives on the {@link Pipe}.
	 */
	protected Triple[] triples;
	
	public CombinedPatternStage(Graph graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		compiled = compile(map, triples);
		guard = makeGuard(map, constraints);
		this.triples=new Triple[triples.length];
	}
	
	/** Compiles <code>triples</code> into <code>compiled</code>. 
	 * A clarified and optimized version of {@link PatternStage}.<code>compile</code>.
	 * @param map a mapping between variable {@link Node}s and {@link Domain} indices.
	 * @param triples a {@link Triple} list containing Jena variable {@link Node}s.
	 * @return for each triple its {@link Pattern} form, where each {@link Node} is
	 * either a {@link Bind}, {@link Bound} or {@link Fixed} {@link Element}.
	 */
	protected Pattern[] compile(Mapping map, Triple[] triples) {
		Pattern[] compiled = new Pattern[triples.length];
		for (tripleNr = 0; tripleNr < triples.length; tripleNr++) {
			Triple t = triples[tripleNr];
			tripleNodes[0]=t.getSubject();
			tripleNodes[1]=t.getPredicate();
			tripleNodes[2]=t.getObject();
			for (nodeNr=0; nodeNr<3; nodeNr++) {
				patternElements[nodeNr]=compileNode(tripleNodes[nodeNr], map);
			}
			compiled[tripleNr] = new Pattern(patternElements[0],patternElements[1],patternElements[2]);
		}
		return compiled;
	}

	/** 
	 * Gets the binding information for a fixed or variable {@link Node}.
	 * This is a condensed version of {@link PatternStageCompiler}'s functionality.
	 * In addition to the {@link PatternStage} version, the indices of sets of elements
	 * of different types are collected in <code>varInfo</code>
	 * For overwriting techniques, see more flexible but less understandable 
	 * solution in {@link PatternStage}.
	 * @param varOrFixedNode the node
	 * @param map the {@link Mapping} which is given by previous stages
	 * @return the compiled {@link Element}.
	 * @see #compiled
	 * @see #varInfo
	 */
	protected Element compileNode(Node varOrFixedNode, Mapping map) {
		if (varOrFixedNode.equals(Query.ANY))
			return Element.ANY;
		if (varOrFixedNode.isVariable()) {
			if (map.hasBound(varOrFixedNode)) {
				varInfo.addBoundNode(varOrFixedNode,map.indexOf(varOrFixedNode),tripleNr,nodeNr);
				return new Bound(map.indexOf(varOrFixedNode));
			} else {
				int domainIndex=map.newIndex(varOrFixedNode);
				varInfo.addBindNode(varOrFixedNode,domainIndex,tripleNr,nodeNr);
				return new Bind(domainIndex);
			}
		}
		return new Fixed(varOrFixedNode);
	}


	/**
	 * Construct a set of {@link Valuator}s from RDQL expressions.
	 * A condensed and corrected version of {@link PatternStage#makeGuards(Mapping, ExpressionSet, int)}.
	 * Answers an ExpressionSet that contains the prepared [against <code>map</code>]
	 * expression that can be evaluated after the triples have matched.
	 * By "can be evaluated" we mean that all its variables are bound.
	 * <p>
	 * Note: makeGuards/canEval() can not work correctly in PatternStage, because makeBoundVariables()
	 * is wrong there. It just unifies all variables that will be bound in this stage, but does
	 * not contain the variables that are bound by a previous stage. So expressions like
	 * x=y where x is bound by previous stage and y by this stage cannot be evaluated.
	 *
	 @param map the Mapping to prepare {@link Expression}s against
	 @param constraints the set of (RDQL) constraint expressions
	 @return the prepared ExpressionSet
	 */
	protected ValuatorSet makeGuard(Mapping map, ExpressionSet constraints) {
		ValuatorSet es = new ValuatorSet();
		Iterator it = constraints.iterator();
		while (it.hasNext()) {
			Expression e = (Expression) it.next();
			if (canEval(map,e)) { 
				Valuator prepared = e.prepare(map);
				es.add(prepared);
				it.remove();
			}
		}
		return es;
	}
	
	/**
	 * Checks if an {@link Expression} can be evaluated when given a {@link Mapping}.
	 * All variables of an expression must be bound before it can be evaluated.
	 * This is a sufficient but not a necessary condition, because in principle some
	 * contradictions can be found even in terms with variables, such as (x=0 AND x=1).
	 * <p>	
	 * We redefined {@link com.hp.hpl.jena.graph.query.PatternStage#canEval(com.hp.hpl.jena.graph.query.Expression, int)} 
	 * so that it looks up the variables of the expression in {@link Mapping}.
	 * After <code>compile()</code> <code>map</code> contains all the variable
	 * bindings of this and the previous stages.
	 * 
	 * @param map the Mapping
	 * @param e a compiled RDQL expression
	 * @return true iff all variables of the expression are bound in <code>map</code>
	 */
    protected boolean canEval( Mapping map, Expression e ) {
		Collection eVars = Expression.Util.variablesOf(e); // Strings
		Iterator varIt=eVars.iterator();
		boolean ok=true;
		while (ok && varIt.hasNext()) {
			String varName=(String)varIt.next();
			Node var=(Node)varInfo.variableNameToNodeMap.get(varName);
			ok=map.hasBound(var); 
		}
		return ok;
    }
    

    /**
     * Realizes the piping between the previous, this and the following {@link Stage}.
     * A new thread is created for this stage.
     * @see PatternStage
     * @see #run(Pipe, Pipe)
     */
 	public Pipe deliver(final Pipe result) {
		final Pipe stream = previous.deliver(new BufferPipe());
		new Thread() {
			public void run() {
				CombinedPatternStage.this.run(stream, result);
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
			varInfo.inputDomain=inputDomain;
			updateTriplesWithDomain(inputDomain);
			
			// get solutions from hook method and put in sink
			ClosableIterator it = resultIteratorForTriplePattern(triples);
			while (stillOpen && it.hasNext()) {
				Triple[] resultTriples = (Triple[]) it.next();
				if (resultTriples.length != triples.length)
					throw new RuntimeException(
							"D2RQPatternStage: PatternQueryCombiner returned triple array with wrong length");
				Domain current = inputDomain; 
				// inputDomain.copy() not necessary. compiled knows in matching, if it should
				// check against (Bound) or update (Bind) the current binding.
				boolean possible = true;
				//  evaluate all matches and guards at once
				for (int index = 0; possible && (index < resultTriples.length); index++) {
					Pattern p = compiled[index];
					possible = p.match(inputDomain, resultTriples[index]);
				}
				guard.evalBool(inputDomain);
				if (possible) {
					sink.put(inputDomain.copy());
				}
			}
			it.close();
		}
		sink.close();
	}
	
	/**
	 * Updates <code>triples</code> with a new set of bindings from the previous stage.
	 * @param inputDomain for each variable number a binding entry
	 */
	public void updateTriplesWithDomain(Domain inputDomain) {
	    // TODO can be more efficient, just check varInfo.boundDomainIndexToShared
		int tripleCount = compiled.length;
		// Triple[] triples = new Triple[tripleCount];
		for (int index = 0; index < tripleCount; index++) {
			Pattern p = compiled[index];
			triples[index] = p.asTripleMatch(inputDomain).asTriple();
		}
	}
		
	/**
	 * It is the subclass' duty to create an iterator.
	 * @param triples all {@link Triple} nodes are fixed or ANY. 
	 * @return iterator that returns instanciations (<code>Triple[]</code>) of the
	 * <code>triples</code> find pattern.
	 */
	abstract ClosableIterator resultIteratorForTriplePattern(Triple[] triples);
}
