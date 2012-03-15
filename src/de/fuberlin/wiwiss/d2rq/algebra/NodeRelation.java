package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;

/**
 * A {@Relation} associated with a number of named {@link NodeMaker}s.
 * 
 * TODO: This is really just a Relation and a BindingMaker wrapped into one. Refactor as such?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeRelation {
	
	public static final NodeRelation TRUE = 
		new NodeRelation(Relation.TRUE, Collections.<String,NodeMaker>emptyMap());

	public static NodeRelation empty(Set<String> variables) {
		Map<String,NodeMaker> map = new HashMap<String,NodeMaker>();
		for (String variableName: variables) {
			map.put(variableName, NodeMaker.EMPTY);
		}
		return new NodeRelation(Relation.EMPTY, map);
	}
	
	private final Relation base;
	private final Map<String,NodeMaker> variablesToNodeMakers;
	
	public NodeRelation(Relation base, Map<String,NodeMaker> variablesToNodeMakers) {
		this.base = base;
		this.variablesToNodeMakers = variablesToNodeMakers;
	}
	
	public Relation baseRelation() {
		return base;
	}
	
	public Set<String> variableNames() {
		return variablesToNodeMakers.keySet();
	}
	
	public NodeMaker nodeMaker(String variableName) {
		return (NodeMaker) variablesToNodeMakers.get(variableName);
	}

	public NodeRelation withPrefix(int index) {
		Collection<Alias> newAliases = new ArrayList<Alias>();
		for (RelationName tableName: baseRelation().tables()) {
			newAliases.add(new Alias(tableName, tableName.withPrefix(index)));
		}
		AliasMap renamer = new AliasMap(newAliases);
		Map<String,NodeMaker> renamedNodeMakers = new HashMap<String,NodeMaker>();
		for (String variableName: variableNames()) {
			renamedNodeMakers.put(variableName, nodeMaker(variableName).renameAttributes(renamer));
		}
		return new NodeRelation(baseRelation().renameColumns(renamer), renamedNodeMakers);
	}
	
	public NodeRelation renameSingleRelation(RelationName oldName, RelationName newName) {
		AliasMap renamer = AliasMap.create1(oldName, newName);
		Map<String,NodeMaker> renamedNodeMakers = new HashMap<String,NodeMaker>();
		
		// This is only done for consistency as the NodeMakers won't be used
		for (String variableName: variableNames()) {
			renamedNodeMakers.put(variableName, nodeMaker(variableName).renameAttributes(renamer));
		}
		return new NodeRelation(baseRelation().renameColumns(renamer), renamedNodeMakers);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("NodeRelation(");
		result.append(base.toString());
		result.append("\n");
		for (String variableName: variableNames()) {
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
