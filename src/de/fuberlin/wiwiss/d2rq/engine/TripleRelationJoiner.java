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

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilterImpl;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class TripleRelationJoiner {
	
	public static TripleRelationJoiner create() {
		return new TripleRelationJoiner(new VariableNameToNodeSetMap(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	/**
	 * Static convenience function that joins several {@link Relation}s into one. Exposed here
	 * because it can be used in the old FastPath engine.
	 * 
	 * TODO: Make private and non-static if no longer needed by old FastPath engine
	 * 
	 * @param relations A set of {@link Relation}s
	 * @param additionalCondition An additional expression, e.g. join condition
	 * @return A relation that is the join of the inputs
	 */
	public static Relation joinRelations(Collection relations, Expression additionalCondition) {
		if (relations.isEmpty()) {
			return Relation.TRUE;
		}
		ConnectedDB connectedDB = ((Relation) relations.iterator().next()).database();
		Iterator it = relations.iterator();
		AliasMap joinedAliases = AliasMap.NO_ALIASES;
		Collection expressions = new HashSet();
		expressions.add(additionalCondition);
		Set joins = new HashSet();
		Set projections = new HashSet();
		while (it.hasNext()) {
			Relation relation = (Relation) it.next();
			joinedAliases = joinedAliases.applyTo(relation.aliases());
			expressions.add(relation.condition());
			joins.addAll(relation.joinConditions());
			projections.addAll(relation.projections());
		}
		return new RelationImpl(connectedDB, joinedAliases, Conjunction.create(expressions), 
				joins, projections, false);
	}
	
	private final VariableNameToNodeSetMap nodeSets;
	private final List joinedTriplePatterns;
	private final List joinedTripleRelations;
	
	private TripleRelationJoiner(VariableNameToNodeSetMap nodeSets, 
			List patterns, List relations) {
		this.nodeSets = nodeSets;
		this.joinedTriplePatterns = patterns;
		this.joinedTripleRelations = relations;
	}
	
	public List joinAll(Triple pattern, List candidates) {
		List results = new ArrayList();
		Iterator it = candidates.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			TripleRelationJoiner nextJoiner = join(pattern, tripleRelation);
			if (nextJoiner != null) {
				results.add(nextJoiner);
			}
		}
		return results;
	}
	
	public TripleRelationJoiner join(Triple pattern, TripleRelation relation) {
		List newPatterns = new ArrayList(joinedTriplePatterns);
		newPatterns.add(pattern);
		List newRelations = new ArrayList(joinedTripleRelations);
		newRelations.add(relation);
		VariableNameToNodeSetMap nodeSets = new VariableNameToNodeSetMap();
		for (int i = 0; i < newPatterns.size(); i++) {
			Triple t = (Triple) newPatterns.get(i);
			TripleRelation r = (TripleRelation) newRelations.get(i);
			nodeSets.registerIfVariable(t.getSubject(), 
					r.nodeMaker(TripleRelation.SUBJECT_NODE_MAKER));
			nodeSets.registerIfVariable(t.getPredicate(), 
					r.nodeMaker(TripleRelation.PREDICATE_NODE_MAKER));
			nodeSets.registerIfVariable(t.getObject(), 
					r.nodeMaker(TripleRelation.OBJECT_NODE_MAKER));
		}
		if (!nodeSets.areAllSatisfiable()) {
			return null;
		}
		return new TripleRelationJoiner(nodeSets, newPatterns, newRelations);
	}
	
	public NodeRelation toNodeRelation() {
		return new NodeRelation(
				joinedBaseRelation().select(
						nodeSets.variableConstraints()).project(nodeSets.projections), 
				nodeSets.nodeMakers());
	}
	
	private Relation joinedBaseRelation() {
		List relations = new ArrayList();
		Iterator it = joinedTripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			relations.add(tripleRelation.baseRelation());
		}
		return joinRelations(relations, Expression.TRUE);
	}
	
	private static class VariableNameToNodeSetMap {
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
			Iterator it = nodeSets.keySet().iterator();
			while (it.hasNext()) {
				String name = (String) it.next();
				NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
				if (nodeSet.isEmpty()) return false;
			}
			return true;
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
