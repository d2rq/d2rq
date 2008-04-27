package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * TODO Merge with TripleRelation
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeRelation.java,v 1.1 2008/04/27 22:42:37 cyganiak Exp $
 */
public class NodeRelation {
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
