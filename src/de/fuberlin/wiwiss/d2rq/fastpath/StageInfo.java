package de.fuberlin.wiwiss.d2rq.fastpath;

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
 * @version $Id: StageInfo.java,v 1.3 2006/10/16 12:46:00 cyganiak Exp $
 */
public class StageInfo {
	private final Mapping mapping; // modified by side effects
	private final VarInfos vars;
	/** For each query triple the compiled node binding information 
	 * ({@link Bind}, {@link Bound}, {@link Fixed}). 
	 */
	private final Pattern[] compiled; // triples with binding information
	/** 
	 * Condition checkers for the conditions that come with the query. They can
	 * be checked after this stage found a matching solution for the variables, 
	 * for example (?x < ?y).
	 * guard[i] is evaluable when the ith triple has matched.
	 */
	private final ValuatorSet guard; // a guard that checks the evaluable expressions from conditions
	
	public StageInfo(Mapping map, ExpressionSet constraints, 
			Triple[] triples) {
		this.mapping = map;
		this.vars = new VarInfos(
				this.mapping, constraints, triples.length);
		this.compiled = compile(triples);
		this.guard = makeGuard(constraints, triples.length - 1);
	}
	
	/** Compiles <code>triples</code> into <code>compiled</code>. 
	 * A clarified and optimized version of {@link PatternStage}.<code>compile</code>.
	 * @param map a mapping between variable {@link Node}s and {@link Domain} indices.
	 * @param triples a {@link Triple} list containing Jena variable {@link Node}s.
	 * @return for each triple its {@link Pattern} form, where each {@link Node} is
	 * either a {@link Bind}, {@link Bound} or {@link Fixed} {@link Element}.
	 */
	private Pattern[] compile(Triple[] triples) {
		Pattern[] compiled = new Pattern[triples.length];
		for (int tripleNr = 0; tripleNr < triples.length; tripleNr++) {
			Triple t = triples[tripleNr];
			compiled[tripleNr] = new Pattern(
					compileNode(t.getSubject(), tripleNr, 0),					
					compileNode(t.getPredicate(), tripleNr, 1),					
					compileNode(t.getObject(), tripleNr, 2));
		}
		return compiled;
	}

	public Pattern compiled(int index) {
		return this.compiled[index];
	}
	
	public VarInfos vars() {
		return this.vars;
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
	 */
	private Element compileNode(Node varOrFixedNode, int tripleNr, int nodeNr) {
		if (varOrFixedNode.equals(Query.ANY))
			return Element.ANY;
		if (varOrFixedNode.isVariable()) {
			if (this.mapping.hasBound(varOrFixedNode)) {
				vars.addBoundNode(varOrFixedNode,this.mapping.indexOf(varOrFixedNode),tripleNr,nodeNr);
				return new Bound(this.mapping.indexOf(varOrFixedNode));
			} else {
				int domainIndex=this.mapping.newIndex(varOrFixedNode);
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
	private ValuatorSet makeGuard(ExpressionSet constraints, int index) {
		ValuatorSet es = new ValuatorSet();
		Iterator it = constraints.iterator();
		while (it.hasNext()) {
			Expression e = (Expression) it.next();
			if (canEval(e, index)) { 
				this.vars.addExpression(e, index);
				Valuator prepared = e.prepare(this.mapping);
				es.add(prepared);
				it.remove();
			}
		}
		return es;
	}

	public boolean evalGuard(Domain domain) {
		return this.guard.evalBool(domain);
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
	private boolean canEval(Expression e, int index) { 
		return Expression.Util.containsAllVariablesOf(vars.boundVariables(index), e); 
	}
}
