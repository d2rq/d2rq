package org.d2rq.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;


/**
 * Produces {@link Binding}s from {@link ResultRow}s. A map from {@link Var}s
 * to {@link NodeMaker}s, plus an optional reference to a column
 * that must be true in the {@link ResultRow} or no binding will be produced
 * from the row.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BindingMaker {
	private final Map<Var,NodeMaker> nodeMakers;
	private final ColumnName conditionColumn;

	public BindingMaker(Map<Var,NodeMaker> nodeMakers) {
		this(nodeMakers, null);
	}
	
	public BindingMaker(Map<Var,NodeMaker> nodeMakers, ColumnName conditionColumn) {
		this.nodeMakers = nodeMakers;
		this.conditionColumn = conditionColumn;
	}

	public Binding makeBinding(ResultRow row) {
		if (conditionColumn != null) {
			String value = row.get(conditionColumn);
			if (value == null || "false".equals(value) || "0".equals(value) || "".equals(value)) {
				return null;
			}
		}
		BindingMap result = new BindingHashMap();
		for (Var variableName: nodeMakers.keySet()) {
			Node node = nodeMakers.get(variableName).makeNode(row);
			if (node == null) {
				return null;
			}
			result.add(Var.alloc(variableName), node);
		}
		return result;
	}
	
	public boolean has(Var variable) {
		return nodeMakers.containsKey(variable);
	}
	
	public Set<Var> variableNames() {
		return nodeMakers.keySet();
	}
	
	
	public NodeMaker get(Var variable) {
		return nodeMakers.get(variable);
	}

	public Map<Var,NodeMaker> getNodeMakers() {
		return nodeMakers;
	}
	
	/**
	 * @return <code>null</code> if the binding maker is not conditional
	 */
	public ColumnName getConditionColumn() {
		return conditionColumn;
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("BindingMaker(\n");
		for (Var variable: nodeMakers.keySet()) {
			result.append("    ");
			result.append(variable);
			result.append(" => ");
			result.append(nodeMakers.get(variable));
			result.append("\n");
		}
		result.append(")");
		if (conditionColumn != null) {
			result.append(" WHERE ");
			result.append(conditionColumn);
		}
		return result.toString();
	}

	public BindingMaker rename(Renamer renamer) {
		Map<Var,NodeMaker> renamedNodeMakers = new HashMap<Var,NodeMaker>();
		for (Var var: nodeMakers.keySet()) {
			renamedNodeMakers.put(var, renamer.applyTo(nodeMakers.get(var)));
		}
		return new BindingMaker(renamedNodeMakers, 
				conditionColumn == null ? null : renamer.applyTo(conditionColumn));
	}
	
	public BindingMaker makeConditional(ColumnName conditionColumn) {
		return new BindingMaker(nodeMakers, conditionColumn);
	}
}
