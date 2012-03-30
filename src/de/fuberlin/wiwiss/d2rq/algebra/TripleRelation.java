package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * A collection of virtual triples obtained by applying a {@link Relation} to a
 * database, and applying {@link NodeMaker}s for subject, predicate and object
 * to each result row. This is implemented as a stateless wrapper around a
 * {@link NodeRelation}.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TripleRelation extends NodeRelation {
	public static final Var SUBJECT = Var.alloc("subject");
	public static final Var PREDICATE = Var.alloc("predicate");
	public static final Var OBJECT = Var.alloc("object");

	private static final Set<Var> SPO = 
		new HashSet<Var>(Arrays.asList(new Var[]{SUBJECT, PREDICATE, OBJECT}));
	
	private static final TripleRelation EMPTY = fromNodeRelation(NodeRelation.empty(SPO));
	
	private static TripleRelation fromNodeRelation(NodeRelation relation) {
		if (relation instanceof TripleRelation) return (TripleRelation) relation;
		if (!relation.variables().equals(SPO)) {
			throw new IllegalArgumentException(
					"Not a TripleRelation header: " + relation.variables());
		}
		return new TripleRelation(relation.baseRelation(), 
				relation.nodeMaker(SUBJECT), relation.nodeMaker(PREDICATE), relation.nodeMaker(OBJECT));
	}
	
	public TripleRelation(Relation baseRelation, 
			final NodeMaker subjectMaker, final NodeMaker predicateMaker, final NodeMaker objectMaker) {
		super(baseRelation, new HashMap<Var,NodeMaker>() {{
			put(SUBJECT, subjectMaker);
			put(PREDICATE, predicateMaker);
			put(OBJECT, objectMaker);
		}});
	}
	
	@Override
	public TripleRelation orderBy(Var variable, boolean ascending) {
		return fromNodeRelation(super.orderBy(variable, ascending));
	}

	@Override
	public TripleRelation limit(int limit) {
		return fromNodeRelation(super.limit(limit));
	}

	public TripleRelation selectTriple(Triple t) {
		MutableRelation newBase = new MutableRelation(baseRelation());
		NodeMaker s = nodeMaker(SUBJECT).selectNode(t.getSubject(), newBase);
		if (s.equals(NodeMaker.EMPTY)) return null;
		NodeMaker p = nodeMaker(PREDICATE).selectNode(t.getPredicate(), newBase);
		if (p.equals(NodeMaker.EMPTY)) return null;
		NodeMaker o = nodeMaker(OBJECT).selectNode(t.getObject(), newBase);
		if (o.equals(NodeMaker.EMPTY)) return null;
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
		projections.addAll(s.projectionSpecs());
		projections.addAll(p.projectionSpecs());
		projections.addAll(o.projectionSpecs());
		newBase.project(projections);
		if (!s.projectionSpecs().isEmpty() && o.projectionSpecs().isEmpty()) {
		    newBase.swapLimits();
		}
		return new TripleRelation(newBase.immutableSnapshot(), s, p, o);
	}

	/**
	 * Creates a {@link NodeRelation} by applying a triple pattern
	 * to the TripleRelation. If the triple contains variables, then
	 * the variables will be bound to the corresponding values
	 * in the resulting NodeRelation. {@link Node#ANY} will be
	 * converted to a variable "subject", "predicate", "object"
	 * depending on its position in the triple pattern.
	 * 
	 * For example, selecting (?x :name ?name) would produce a
	 * NodeRelation with ?x bound to the subjects and ?name bound to the
	 * objects of this TripleRelation.
	 * 
	 * TODO This is never called at the moment. Why!?
	 * 
	 * @param t A triple pattern involving variables
	 * @return A NodeRelation over the variables occurring in the triple pattern 
	 */
	public TripleRelation selectWithVariables(Triple t) {
		// Select non-variable parts of the triple
		TripleRelation selected = selectTriple(t);
		
		// Replace Node.ANY with "subject", "predicate", "object"
		Node s = t.getSubject() == Node.ANY ? SUBJECT : t.getSubject();
		Node p = t.getPredicate() == Node.ANY ? PREDICATE : t.getPredicate();
		Node o = t.getObject() == Node.ANY ? OBJECT : t.getObject();
		
		// Collect variable names and their bound node makers 
		VariableConstraints nodeMakers = new VariableConstraints();
		nodeMakers.addIfVariable(s, nodeMaker(SUBJECT), baseRelation().aliases());
		nodeMakers.addIfVariable(p, nodeMaker(PREDICATE), baseRelation().aliases());
		nodeMakers.addIfVariable(o, nodeMaker(OBJECT), baseRelation().aliases());

		// Did the same variable occur more than once in the pattern, rendering it unsatisfiable?
		if (!nodeMakers.satisfiable()) {
			return TripleRelation.EMPTY;
		}
		
		MutableRelation mutator = new MutableRelation(selected.baseRelation());

		Expression constraint = nodeMakers.constraint();
		if (!constraint.isTrue()) {
			// Same variable occured more than once, so we add a constraint to the base relation 
			mutator.select(constraint);
		}
		
		mutator.project(nodeMakers.allProjections());
		return fromNodeRelation(new NodeRelation(
				mutator.immutableSnapshot(), nodeMakers.toMap()));
	}
}