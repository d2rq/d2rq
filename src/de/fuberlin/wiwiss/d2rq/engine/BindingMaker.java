package de.fuberlin.wiwiss.d2rq.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;

import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Produces {@link Binding}s from {@link ResultRow}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BindingMaker {
	private final Map variableNamesToNodeMakers;
	private final ProjectionSpec condition;
	
	public BindingMaker(BindingMaker other, ProjectionSpec condition) {
		this.variableNamesToNodeMakers = other.variableNamesToNodeMakers;
		this.condition = condition;
	}
	
	public BindingMaker(NodeRelation nodeRelation) {
		variableNamesToNodeMakers = new HashMap();
		Iterator it = nodeRelation.variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			variableNamesToNodeMakers.put(variableName, 
					nodeRelation.nodeMaker(variableName));
		}
		condition = null;
	}
	
	public Binding makeBinding(ResultRow row) {
		if (condition != null) {
			String value = row.get(condition);
			if (value == null || "false".equals(value) || "0".equals(value) || "".equals(value)) {
				return null;
			}
		}
		BindingMap result = new BindingHashMap();
		Iterator it = variableNamesToNodeMakers.keySet().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			NodeMaker nodeMaker = (NodeMaker) variableNamesToNodeMakers.get(variableName);
			Node node = nodeMaker.makeNode(row);
			if (node == null) {
				return null;
			}
			result.add(Var.alloc(variableName), node);
		}
		return result;
	}
	
	public Set variableNames()
	{
		return variableNamesToNodeMakers.keySet();
	}
	
	
	public NodeMaker nodeMaker(Var var)
	{
		return (NodeMaker) variableNamesToNodeMakers.get(var.getName());
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BindingMaker(\n");
		Iterator it = variableNamesToNodeMakers.keySet().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			result.append("    ");
			result.append(variableName);
			result.append(" => ");
			result.append(variableNamesToNodeMakers.get(variableName));
			result.append("\n");
		}
		result.append(")");
		if (condition != null) {
			result.append(" WHERE ");
			result.append(condition);
		}
		return result.toString();
	}
}
