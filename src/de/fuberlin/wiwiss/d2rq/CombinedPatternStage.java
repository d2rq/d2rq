
package de.fuberlin.wiwiss.d2rq;

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

import de.fuberlin.wiwiss.d2rq.helpers.VariableBindings;
import de.fuberlin.wiwiss.d2rq.helpers.VariableIndex;

/**
 A PatternStage is a Stage that handles some bunch of related patterns; those patterns
 are encoded as Triples.
 
 @author hedgehog
 */

// logic behind this (example):
// An RDQL query (triples) contains nodes, 
// some of which are (shared) variables (a-z). 
// This Stage only sees variables a,b,c,i,j,k,x,y,z
// The type of the nodes (fixed, bound or bind Element) is put into compiled. 
// The mapping lists variables a,b,c...h (points to Domain indices)
// that will be bound by a previous stage and put into the Pipe. (Bound)
// This stage will pick up each binding (on its own thread), 
// substitute variables with existing bindings, and produce additional bindings.
// Some variables will still not be bound and left over for next stage.
// This stage is first to bind some variables i,j,k,x,y,z (1st time Bind)
// Some variables x,y,z are used in more than one place => (2nd time: Bound) 
// We can not just replace bound variables by fixed nodes, if they are shared.
// CombinedPatternStage is a modified version of PatternStage
public abstract class CombinedPatternStage extends Stage {
	protected Pattern[] compiled; // triples with binding information
	protected ValuatorSet guard; // a guard that checks the evaluable expressions from conditions
    protected VariableBindings varInfo=new VariableBindings();

    // used in compile()
	int tripleNr, nodeNr;
	Node tripleNodes[]=new Node[3]; // helper variable used for iterations
	Element patternElements[]=new Element[3];

	// used in run()
	Triple[] triples;
	
	public CombinedPatternStage(Graph graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		compiled = compile(map, triples);
		guard = makeGuard(map, constraints);
		this.triples=new Triple[triples.length];
	}
	
	// same as PatternStage
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

	// condensed version of PatternStageCompiler's functionality
	// for overwriting techniques, see (too) flexible solution in PatternStage
	protected Element compileNode(Node X, Mapping map) {
		if (X.equals(Query.ANY))
			return Element.ANY;
		if (X.isVariable()) {
			if (map.hasBound(X)) {
				varInfo.addBoundNode(X,map.indexOf(X),tripleNr,nodeNr);
				return new Bound(map.indexOf(X));
			} else {
				int domainIndex=map.newIndex(X);
				varInfo.addBindNode(X,domainIndex,tripleNr,nodeNr);
				return new Bind(domainIndex);
			}
		}
		return new Fixed(X);
	}


	/**
	 Answer an ExpressionSet that contains the prepared [against <code>map</code>]
	 expression that can be evaluated after the triples have matched.
	 By "can be evaluated" we mean that all its variables are bound.
	 
	 jg: makeGuards/canEval() can not work correctly in PatternStage, because makeBoundVariables()
	 is wrong. It just unifies all variables that will be bound in this stage, but does
	 not contain the variables that are bound by a previous stage. So expressions like
	 x=y where x is bound by previous stage and y by this stage cannot be evaluated here.
	 
	 @param map the Mapping to prepare Expressions against
	 @param constraints the set of constraint expressions to plant
	 @param length the number of evaluation slots available
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
	
	// We redefine canEval() so that it looks up the variables of an expression in map.

    protected boolean canEval( Mapping map, Expression e ) {
		Collection eVars = Expression.Util.variablesOf(e);
		Iterator varIt=eVars.iterator();
		boolean ok=true;
		while (ok && varIt.hasNext()) {
			Node var=(Node)varIt.next();
			ok=map.hasBound(var); 
		}
		return ok;
    }


	// same as PatternStage
	public Pipe deliver(final Pipe result) {
		final Pipe stream = previous.deliver(new BufferPipe());
		new Thread() {
			public void run() {
				CombinedPatternStage.this.run(stream, result);
			}
		}.start();
		return result;
	}

	//  a,b,c get bound by source. They are substituted by asTripleMatch(). (Bound)
	//  i,j,k,x,y,z  are bound by p.match() the first time they are seen. (Bind)
	//  x,y,z are checked by p.match() the second time they are seen. (Bound)

	// overridden from PatternStage (includes elements from run() and nest())
	protected void run(Pipe source, Pipe sink) {
		while (stillOpen && source.hasNext()) {
			Domain inputDomain = source.get();
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
	
	public void updateTriplesWithDomain(Domain inputDomain) {
		int tripleCount = compiled.length;
		// Triple[] triples = new Triple[tripleCount];
		for (int index = 0; index < tripleCount; index++) {
			Pattern p = compiled[index];
			triples[index] = p.asTripleMatch(inputDomain).asTriple();
		}
	}
	
	abstract ClosableIterator resultIteratorForTriplePattern(Triple[] triples);
}
