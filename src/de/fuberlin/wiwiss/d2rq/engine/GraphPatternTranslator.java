package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilterImpl;

public class GraphPatternTranslator {
	private final List triplePatterns;
	private final Collection tripleRelations;
	private final Set variableNames = new HashSet();

	public GraphPatternTranslator(List triplePatterns, Collection tripleRelations) {
		this.triplePatterns = triplePatterns;
		this.tripleRelations = tripleRelations;
		Iterator it = triplePatterns.iterator();
		while (it.hasNext()) {
			Triple triple = (Triple) it.next();
			addVariableName(triple.getSubject());
			addVariableName(triple.getPredicate());
			addVariableName(triple.getObject());
		}
	}

	private void addVariableName(Node node) {
		if (!node.isVariable()) return;
		variableNames.add(node.getName());
	}
	
	public List translate() {
		if (triplePatterns.isEmpty()) {
			return Collections.singletonList(
					new NodeRelation(Relation.TRUE, Collections.EMPTY_MAP));
		}
		List nodeRelations = new ArrayList();
		Iterator it = tripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			NodeRelation translated = translate(tripleRelation);
			if (translated != null) {
				nodeRelations.add(translated);
			}
		}
		return nodeRelations;
	}

	private NodeRelation translate(TripleRelation tripleRelation) {
		TripleRelation tr = (TripleRelation) tripleRelations.iterator().next();
		Triple t = (Triple) triplePatterns.get(0);
		TripleRelation selected = tr.selectTriple(t);
		if (selected == null) {
			return null;
		}
		VariableNameToNodeSetMap nodeSets = new VariableNameToNodeSetMap();
		nodeSets.registerIfVariable(t.getSubject(), 
				selected.nodeMaker(TripleRelation.SUBJECT_NODE_MAKER));
		nodeSets.registerIfVariable(t.getPredicate(), 
				selected.nodeMaker(TripleRelation.PREDICATE_NODE_MAKER));
		nodeSets.registerIfVariable(t.getObject(), 
				selected.nodeMaker(TripleRelation.OBJECT_NODE_MAKER));
		if (!nodeSets.areAllSatisfiable()) {
			return null;
		}
		return new NodeRelation(
				selected.baseRelation().select(
						nodeSets.variableConstraints()).project(nodeSets.projections), 
				nodeSets.nodeMakers());
	}
	
	private class VariableNameToNodeSetMap {
		private final Map nodeSets = new HashMap();
		private final Map nodeMakers = new HashMap();
		private final Set projections = new HashSet();
		void registerIfVariable(Node node, NodeMaker nodeMaker) {
			if (!node.isVariable()) return;
			String name = node.getName();
			if (!nodeMakers.containsKey(name)) {
				nodeMakers.put(name, nodeMaker);
				projections.addAll(nodeMaker.projectionSpecs());
			}
			if (!nodeSets.containsKey(name)) {
				nodeSets.put(name, new NodeSetFilterImpl());
			}
			NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
			nodeMaker.describeSelf(nodeSet);
		}
		boolean areAllSatisfiable() {
			boolean hasEmptySet = false;
			Iterator it = nodeSets.keySet().iterator();
			while (it.hasNext()) {
				String name = (String) it.next();
				NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
				hasEmptySet = hasEmptySet || nodeSet.isEmpty();
			}
			return !hasEmptySet;
		}
		Expression variableConstraints() {
			Collection expressions = new ArrayList();
			Iterator it = nodeSets.keySet().iterator();
			while (it.hasNext()) {
				String name = (String) it.next();
				NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
				if (!nodeSet.isEmpty()) {
					expressions.add(nodeSet.translatedExpression());
				}
			}
			return Conjunction.create(expressions);
		}
		Map nodeMakers() {
			return nodeMakers;
		}
		Set projections() {
			return projections;
		}
	}
}
