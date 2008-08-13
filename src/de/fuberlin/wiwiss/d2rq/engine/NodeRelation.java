package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeRelation.java,v 1.5 2008/08/13 06:34:59 cyganiak Exp $
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

	public NodeRelation withPrefix(int index) {
		Collection newAliases = new ArrayList();
		Iterator it = baseRelation().tables().iterator();
		while (it.hasNext()) {
			RelationName tableName = (RelationName) it.next();
			newAliases.add(new Alias(tableName, tableName.withPrefix(index)));
		}
		AliasMap renamer = new AliasMap(newAliases);
		Map renamedNodeMakers = new HashMap();
		it = variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			renamedNodeMakers.put(variableName, nodeMaker(variableName).renameAttributes(renamer));
		}
		return new NodeRelation(baseRelation().renameColumns(renamer), renamedNodeMakers);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("NodeRelation(");
		result.append(base.toString());
		result.append("\n");
		Iterator it = variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			result.append("    ");
			result.append(variableName);
			result.append(" => ");
			result.append(nodeMaker(variableName).toString());
			result.append("\n");
		}
		result.append(")");
		return result.toString();
	}
}
