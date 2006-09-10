package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Bind;
import com.hp.hpl.jena.graph.query.Bound;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Element;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Fixed;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.PatternStageCompiler;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.Valuator;
import com.hp.hpl.jena.graph.query.ValuatorSet;

/**
 * @author jgarbers
 * @version $Id: StageInfo.java,v 1.4 2006/09/10 22:18:44 cyganiak Exp $
 */
public class StageInfo {
	
	protected VarInfos vars;
	
	/** For each query triple the compiled node binding information ({@link Bind}, {@link Bound}, {@link Fixed}). */
	protected Pattern[] compiled; // triples with binding information
	/** Condition checkers for the conditions that come with the query. They can
	 * be checked after this stage found a matching solution for the variables, 
	 * for example (?x < ?y).
	 * guard[i] is evaluable when the ith triple has matched.
	 */
	protected ValuatorSet guard; // a guard that checks the evaluable expressions from conditions
	protected ValuatorSet[] guards; 
	
    // inter-method variables and helper variables used in compile()
	private int tripleNr, nodeNr;
	private Node tripleNodes[]=new Node[3]; // helper variable used for iterations

	// used in setup()
	protected Mapping queryMapping; // modified by side effects
	protected Triple[] queryTriples;
	protected ExpressionSet queryConstraints; // modified by side effects
	
	public StageInfo(Mapping map,
			ExpressionSet constraints, Triple[] triples) {
	    this.queryMapping=map;
	    this.queryConstraints=constraints;
	    this.queryTriples=triples;
	}
		
	public void setupForPartsProcessing() {
		vars=makeInitialVarInfos(true);
		vars.allocParts();
		compiled = compile(queryMapping, queryTriples);
		guards = makeGuards(queryMapping, queryConstraints,queryTriples.length);	    
	}
	public void setupForAllProcessing() {
		vars=makeInitialVarInfos(false);
		compiled = compile(queryMapping, queryTriples);
		guard = makeGuard(queryMapping, queryConstraints);	    
	}
	
	/**
	 * Sets up vars.
	 */
	public VarInfos makeInitialVarInfos(boolean allocateParts) {
		VarInfos v=new VarInfos(queryMapping,queryConstraints,queryTriples.length);
		if (allocateParts) {
			v.allocParts();
		}
		return v;
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
			Element[] patternElements = new Element[3];
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
	 * @see #vars
	 */
	protected Element compileNode(Node varOrFixedNode, Mapping map) {
		if (varOrFixedNode.equals(Query.ANY))
			return Element.ANY;
		if (varOrFixedNode.isVariable()) {
			if (map.hasBound(varOrFixedNode)) {
				vars.addBoundNode(varOrFixedNode,map.indexOf(varOrFixedNode),tripleNr,nodeNr);
				return new Bound(map.indexOf(varOrFixedNode));
			} else {
				int domainIndex=map.newIndex(varOrFixedNode);
				vars.addBindNode(varOrFixedNode,domainIndex,tripleNr,nodeNr);
				return new Bind(domainIndex);
			}
		}
		return new Fixed(varOrFixedNode);
	}

	/** Construct a set of {@link Valuator}s from RDQL expressions.
	 * A condensed and corrected version of PatternStage.
	 * Answers an ExpressionSet that contains the prepared [against <code>map</code>]
	 * expression that can be evaluated after the triples have matched.
	 * By "can be evaluated" we mean that all its variables are bound.
	 * <p>
	 * Note: makeGuards/canEval() can not work correctly in Jena 2.2 PatternStage, because makeBoundVariables()
	 * is wrong there. It just unifies all variables that will be bound in this stage, but does
	 * not contain the variables that are bound by a previous stage. So expressions like
	 * x=y where x is bound by previous stage and y by this stage cannot be evaluated.
	 *
	 * Note: PatternStage in Jena 2.3: makeGuard()-Problem continues to exist:
	 * GuardArranger still omits the variables that are bound in previous stages.
	 * GuardArranger.canEval checks only for boundVariables (stemming from these triples).
	 *
	 * @param map the Mapping to prepare {@link Expression}s against
	 * @param constraints the set of (RDQL) constraint expressions
	 * @return the prepared ExpressionSet
	 */
	protected ValuatorSet makeGuard(Mapping map, ExpressionSet constraints) {
		ValuatorSet es = new ValuatorSet();
		Iterator it = constraints.iterator();
		int i=queryTriples.length-1;
		while (it.hasNext()) {
			Expression e = (Expression) it.next();
			boolean evaluable=canEval(e,i);
			if (evaluable) { 
			    vars.addExpression(e,i);
				Valuator prepared = e.prepare(map);
				es.add(prepared);
				it.remove();
			}
		}
		return es;
	}
	

    /**
    Answer an array of ExpressionSets exactly as long as the supplied length.
    The i'th ExpressionSet contains the prepared [against <code>map</code>]
    expressions that can be evaluated as soon as the i'th triple has been matched.
    By "can be evaluated as soon as" we mean that all its variables are bound.
    The original ExpressionSet is updated by removing those elements that can
    be so evaluated.
    
 	@param map the Mapping to prepare Expressions against
 	@param constraints the set of constraint expressions to plant
 	@param length the number of evaluation slots available
 	@return the array of prepared ExpressionSets
 	*/
	protected ValuatorSet[] makeGuards(Mapping map, ExpressionSet constraints,
			int length) {
		ValuatorSet[] result = new ValuatorSet[length];
		for (int i = 0; i < length; i += 1)
			result[i] = new ValuatorSet();
		Iterator it = constraints.iterator();
		while (it.hasNext())
			plantWhereFullyBound((Expression) it.next(), it, map, result);
		return result;
	}
	
    /**
    Find the earliest triple index where this expression can be evaluated, add it
    to the appropriate expression set, and remove it from the original via the
    iterator.
    Note: Side effects on ValuatorSet represented by iterator.
    */
	protected void plantWhereFullyBound(Expression e, Iterator it, Mapping map,
			ValuatorSet[] es) {
		for (int i = 0; i < vars.boundVariables.length; i += 1) {
			boolean evaluable=canEval(e,i);
			if (evaluable) { 
			    vars.addExpression(e,i);
				Valuator prepared = e.prepare(map);
				es[i].add(prepared);
				it.remove();
				return;
			}
		}
	}
	
	/**
	 * Checks if an {@link Expression} can be evaluated after the index'th triple has been matched.
	 * All variables of an expression must be bound before it can be evaluated.
	 * This is a sufficient but not a necessary condition, because in principle some
	 * contradictions can be found even in terms with variables, such as (x=0 AND x=1).
	 * <p>	
	 * We redefined PatternStage 
	 * so that it also considers variables bound in previous stages.
	 * @param e a compiled RDQL expression
	 * @return true iff all variables of the expression are bound in <code>map</code>
	 */
    protected boolean canEval( Expression e, int index )
    { 
    	return Expression.Util.containsAllVariablesOf( vars.boundVariables[index], e ); 
    }
    	
}
