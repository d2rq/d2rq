package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilterImpl;

/**
 * A map from variable names to {@link NodeMaker}s that helps to build up
 * constraints in cases where a variable is bound multiple times.
 * 
 * If several node makers are added for the same variable name, then only
 * one will be kept, and a constraint expression will be built up. The
 * expression ensures that both identically-named node makers produce the
 * same node for any result row that matches the expression. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NamesToNodeMakersMap {
	private final Map<String,NodeSetFilter> nodeSets = new HashMap<String,NodeSetFilter>();
	private final Map<String,NodeMaker> nodeMakers = new HashMap<String,NodeMaker>();
	private final Map<String,AliasMap> nodeRelationAliases = new HashMap<String,AliasMap>();
	private final Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();

	public void add(String name, NodeMaker nodeMaker, AliasMap aliases) {
		if (!nodeMakers.containsKey(name)) {
			nodeMakers.put(name, nodeMaker);
			projections.addAll(nodeMaker.projectionSpecs());
		}
		if (!nodeSets.containsKey(name)) {
			nodeSets.put(name, new NodeSetFilterImpl());
		}
		NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
		nodeMaker.describeSelf(nodeSet);
		if (!nodeRelationAliases.containsKey(name)) {
			nodeRelationAliases.put(name, aliases);
		}
	}
	
	public void addIfVariable(Node possibleVariable, NodeMaker nodeMaker, AliasMap aliases) {
		if (!possibleVariable.isVariable()) return;
		add(possibleVariable.getName(), nodeMaker, aliases);
	}

	public void addAll(NodeRelation nodeRelation) {
		for (String variableName: nodeRelation.variableNames()) {
			add(variableName, nodeRelation.nodeMaker(variableName), nodeRelation.baseRelation().aliases());
		}
	}
	
	/**
	 * @return <tt>false</tt> if two identically-named node makers cannot produce the same node
	 */
	public boolean satisfiable() {
		return !constraint().isFalse();
	}

	/**
	 * @return An expression that, if it holds for a result row, will ensure that
	 * 		any two identically-named node makers produce the same node
	 */
	public Expression constraint() {
		Collection<Expression> expressions = new ArrayList<Expression>();
		for (String name: nodeSets.keySet()) {
			NodeSetFilterImpl nodeSet = (NodeSetFilterImpl) nodeSets.get(name);
			if (nodeSet.isEmpty()) {
				return Expression.FALSE;
			}
			expressions.add(nodeSet.constraint());
		}
		return Conjunction.create(expressions);
	}

	/**
	 * @return A regular map from variable names to {@link NodeMaker}s
	 */
	public Map<String,NodeMaker> toMap() {
		return nodeMakers;
	}

	public Set<String> allNames() {
		return nodeMakers.keySet();
	}
	
	public Map<String,AliasMap> relationAliases() {
		return nodeRelationAliases;
	}

	/**
	 * @return All projections needed by any of the retained node makers 
	 */
	public Set<ProjectionSpec> allProjections() {
		return projections;
	}
}
