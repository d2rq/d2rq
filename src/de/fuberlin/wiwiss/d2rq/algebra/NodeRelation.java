package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
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
	
	/**
	 * Joins this NodeRelation with a Binding. Any row in this
	 * NodeRelation that is incompatible with the binding will be
	 * dropped, and any compatible row will be extended with
	 * FixedNodeMakers whose node is taken from the binding.
	 * 
	 * @param binding A binding to join with this NodeRelation
	 * @return The joined NodeRelation
	 */
	public NodeRelation extendWith(Binding binding) {
		if (binding.isEmpty()) return this;
		MutableRelation mutator = new MutableRelation(baseRelation());
		Map<String,NodeMaker> columns = new HashMap<String,NodeMaker>();
		for (String varName: variableNames()) {
			columns.put(varName, nodeMaker(varName));
		}
		for (Iterator<Var> it = binding.vars(); it.hasNext();) {
			Var var = it.next();
			Node value = binding.get(var);
			if (columns.containsKey(var.getName())) {
				columns.put(var.getName(), columns.get(var.getName()).selectNode(value, mutator));
			} else {
				columns.put(var.getName(), new FixedNodeMaker(value, false));
			}
		}
		return new NodeRelation(mutator.immutableSnapshot(), columns);
	}
	
	public NodeRelation select(Expression expression) {
        MutableRelation mutator = new MutableRelation(baseRelation());
        mutator.select(expression);
        return new NodeRelation(mutator.immutableSnapshot(), variablesToNodeMakers);
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
