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
 * @version $Id: NodeRelation.java,v 1.7 2009/08/02 09:15:08 fatorange Exp $
 */
public class NodeRelation {
	
	public static final NodeRelation TRUE = 
		new NodeRelation(Relation.TRUE, Collections.EMPTY_MAP);

	public static NodeRelation empty(Set variables) {
		Map map = new HashMap();
		Iterator it = variables.iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			map.put(variableName, NodeMaker.EMPTY);
		}
		return new NodeRelation(Relation.EMPTY, map);
	}
	
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
	
	public NodeRelation renameSingleRelation(RelationName oldName, RelationName newName) {
		AliasMap renamer = AliasMap.create1(oldName, newName);
		Map renamedNodeMakers = new HashMap();
		
		// This is only done for consistency as the NodeMakers won't be used
		Iterator it = variableNames().iterator();
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
