package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * TODO Merge with TripleRelation
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeRelation.java,v 1.2 2008/04/28 14:33:21 cyganiak Exp $
 */
public class NodeRelation {
	
	public static final NodeRelation TRUE = 
		new NodeRelation(Relation.TRUE, Collections.EMPTY_MAP);
	
	private Relation base;
	private Map variablesToNodeMakers;
	
	public NodeRelation(Relation base, Map variablesToNodeMakers) {
		this.base = base;
		this.variablesToNodeMakers = variablesToNodeMakers;
	}
	
	public Relation baseRelation() {
		return base;
	}
	
	public Set variableNames() {
		return variablesToNodeMakers.keySet();
	}
	
	public NodeMaker nodeMaker(String variableName) {
		return (NodeMaker) variablesToNodeMakers.get(variableName);
	}
}
