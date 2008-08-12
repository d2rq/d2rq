package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
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
 * @version $Id: TripleRelation.java,v 1.9 2008/08/12 17:26:22 cyganiak Exp $
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
		return new TripleRelation(newBase.immutableSnapshot(), s, p, o);
	}
}