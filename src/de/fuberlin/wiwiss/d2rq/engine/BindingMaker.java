package de.fuberlin.wiwiss.d2rq.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;

import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Produces {@link Binding}s or {@link Triple}s from {@link ResultRow}s.
 * 
 * A triple-producing binding maker is simply a binding maker
 * that contains exactly the three variables
 * {@link #SUBJECT}, {@link #PREDICATE} and {@link #OBJECT}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BindingMaker {
	private final static Var SUBJECT = Var.alloc(TripleRelation.SUBJECT);
	private final static Var PREDICATE = Var.alloc(TripleRelation.PREDICATE);
	private final static Var OBJECT = Var.alloc(TripleRelation.OBJECT);

	public static BindingMaker createFor(NodeRelation relation) {
		Map<String, NodeMaker> vars = new HashMap<String,NodeMaker>();
		for (String variableName: relation.variableNames()) {
			vars.put(variableName, relation.nodeMaker(variableName));
		}
		return new BindingMaker(vars, null);
	}

	private final Map<String,NodeMaker> variableNamesToNodeMakers;
	private final ProjectionSpec condition;

	public BindingMaker(Map<String,NodeMaker> variableNamesToNodeMakers, ProjectionSpec condition) {
		this.variableNamesToNodeMakers = variableNamesToNodeMakers;
		this.condition = condition;
	}

	public Binding makeBinding(ResultRow row) {
		if (condition != null) {
			String value = row.get(condition);
			if (value == null || "false".equals(value) || "0".equals(value) || "".equals(value)) {
				return null;
			}
		}
		BindingMap result = new BindingHashMap();
		for (String variableName: variableNamesToNodeMakers.keySet()) {
			Node node = variableNamesToNodeMakers.get(variableName).makeNode(row);
			if (node == null) {
				return null;
			}
			result.add(Var.alloc(variableName), node);
		}
		return result;
	}
	
	public Triple makeTriple(ResultRow row) {
		Binding b = makeBinding(row);
		if (b == null) return null;
		return new Triple(b.get(SUBJECT), b.get(PREDICATE), b.get(OBJECT));
	}
	
	public Set<String> variableNames() {
		return variableNamesToNodeMakers.keySet();
	}
	
	
	public NodeMaker nodeMaker(String var) {
		return variableNamesToNodeMakers.get(var);
	}
	
	public ProjectionSpec condition() {
		return condition;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BindingMaker(\n");
		for (String variableName: variableNamesToNodeMakers.keySet()) {
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
	
	public BindingMaker makeConditional(ProjectionSpec condition) {
		return new BindingMaker(variableNamesToNodeMakers, condition);
	}
}
