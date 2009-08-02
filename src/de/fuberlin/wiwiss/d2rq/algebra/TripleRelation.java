package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.engine.NamesToNodeMakersMap;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * A collection of virtual triples obtained by applying a {@link Relation} to a
 * database, and applying {@link NodeMaker}s for subject, predicate and object
 * to each result row. This is implemented as a stateless wrapper around a
 * {@link NodeRelation}.
 *
 * TODO Merge with {@link NodeRelation}; only selectTriple and makeTriples are troublesome
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleRelation.java,v 1.12 2009/08/02 09:15:09 fatorange Exp $
 */
public class TripleRelation {
	public static final String SUBJECT = "subject";
	public static final String PREDICATE = "predicate";
	public static final String OBJECT = "object";

	public static final List SUBJ_PRED_OBJ = Arrays.asList(
			new String[]{SUBJECT, PREDICATE, OBJECT});
	
	private NodeRelation nodeRelation;
	
	public TripleRelation(Relation baseRelation, 
			final NodeMaker subjectMaker, final NodeMaker predicateMaker, final NodeMaker objectMaker) {
		this(new NodeRelation(baseRelation, new HashMap() {{
			put(SUBJECT, subjectMaker);
			put(PREDICATE, predicateMaker);
			put(OBJECT, objectMaker);
		}}));
	}
	
	private TripleRelation(NodeRelation nodeRelation) {
		if (!nodeRelation.variableNames().equals(new HashSet(SUBJ_PRED_OBJ))) {
			throw new IllegalArgumentException(
					"Not a TripleRelation header: " + nodeRelation.variableNames());
		}
		this.nodeRelation = nodeRelation;
	}
	
	public String toString() {
		return "TripleRelation(\n" +
				"    " + nodeMaker(SUBJECT) + "\n" +
				"    " + nodeMaker(PREDICATE) + "\n" +
				"    " + nodeMaker(OBJECT) + "\n" +
				")";
	}
	
	public Relation baseRelation() {
		return nodeRelation.baseRelation();
	}
	
	public NodeMaker nodeMaker(String variableName) {
		return nodeRelation.nodeMaker(variableName);
	}
	
	public TripleRelation withPrefix(int index) {
		return new TripleRelation(nodeRelation.withPrefix(index));
	}
	
	public TripleRelation renameSingleRelation(RelationName oldName, RelationName newName) {
		return new TripleRelation(nodeRelation.renameSingleRelation(oldName, newName));
	}
	
	public TripleRelation selectTriple(Triple t) {
		MutableRelation newBase = new MutableRelation(baseRelation());
		NodeMaker s = nodeMaker(SUBJECT).selectNode(t.getSubject(), newBase);
		if (s.equals(NodeMaker.EMPTY)) return null;
		NodeMaker p = nodeMaker(PREDICATE).selectNode(t.getPredicate(), newBase);
		if (p.equals(NodeMaker.EMPTY)) return null;
		NodeMaker o = nodeMaker(OBJECT).selectNode(t.getObject(), newBase);
		if (o.equals(NodeMaker.EMPTY)) return null;
		Set projections = new HashSet();
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
	 * @param t A triple pattern involving variables
	 * @return A NodeRelation over the variables occurring in the triple pattern 
	 */
	public NodeRelation selectWithVariables(Triple t) {
		// Select non-variable parts of the triple
		TripleRelation selected = selectTriple(t);
		
		// Replace Node.ANY with "subject", "predicate", "object"
		Node s = t.getSubject() == Node.ANY ? Node.createVariable(SUBJECT) : t.getSubject();
		Node p = t.getPredicate() == Node.ANY ? Node.createVariable(PREDICATE) : t.getPredicate();
		Node o = t.getObject() == Node.ANY ? Node.createVariable(OBJECT) : t.getObject();
		
		// Collect variable names and their bound node makers 
		NamesToNodeMakersMap nodeMakers = new NamesToNodeMakersMap();
		nodeMakers.addIfVariable(s, nodeMaker(SUBJECT), nodeRelation.baseRelation().aliases());
		nodeMakers.addIfVariable(p, nodeMaker(PREDICATE), nodeRelation.baseRelation().aliases());
		nodeMakers.addIfVariable(o, nodeMaker(OBJECT), nodeRelation.baseRelation().aliases());

		// Did the same variable occur more than once in the pattern, rendering it unsatisfiable?
		if (!nodeMakers.satisfiable()) {
			return NodeRelation.empty(nodeMakers.allNames());
		}
		
		MutableRelation mutator = new MutableRelation(selected.baseRelation());

		Expression constraint = nodeMakers.constraint();
		if (!constraint.isTrue()) {
			// Same variable occured more than once, so we add a constraint to the base relation 
			mutator.select(constraint);
		}
		
		mutator.project(nodeMakers.allProjections());
		return new NodeRelation(mutator.immutableSnapshot(), nodeMakers.toMap());
	}
}