package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
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
 * @version $Id: NamesToNodeMakersMap.java,v 1.1 2008/08/13 11:27:35 cyganiak Exp $
 */
public class NamesToNodeMakersMap {
	private final Map nodeSets = new HashMap();
	private final Map nodeMakers = new HashMap();
	private final Set projections = new HashSet();

	public void add(String name, NodeMaker nodeMaker) {
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
	
	public void addIfVariable(Node possibleVariable, NodeMaker nodeMaker) {
		if (!possibleVariable.isVariable()) return;
		add(possibleVariable.getName(), nodeMaker);
	}

	public void addAll(NodeRelation nodeRelation) {
		Iterator it = nodeRelation.variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			add(variableName, nodeRelation.nodeMaker(variableName));
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
		Collection expressions = new ArrayList();
		Iterator it = nodeSets.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
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
	public Map toMap() {
		return nodeMakers;
	}

	public Set allNames() {
		return nodeMakers.keySet();
	}
	
	/**
	 * @return All projections needed by any of the retained node makers 
	 */
	public Set allProjections() {
		return projections;
	}
}
