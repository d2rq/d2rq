package de.fuberlin.wiwiss.d2rq.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;

import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Produces {@link Binding}s from {@link ResultRow}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: BindingMaker.java,v 1.1 2008/08/13 06:34:59 cyganiak Exp $
 */
public class BindingMaker {
	private final Map variableNamesToNodeMakers;
	
	public BindingMaker(Map variableNamesToNodeMakers) {
		this.variableNamesToNodeMakers = variableNamesToNodeMakers;
	}
	
	public BindingMaker(NodeRelation nodeRelation) {
		variableNamesToNodeMakers = new HashMap();
		Iterator it = nodeRelation.variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			variableNamesToNodeMakers.put(variableName, 
					nodeRelation.nodeMaker(variableName));
		}
	}
	
	public Binding makeBinding(ResultRow row) {
		BindingMap result = new BindingMap();
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
		return result.toString();
	}
}
