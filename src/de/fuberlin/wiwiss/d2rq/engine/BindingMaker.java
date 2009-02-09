package de.fuberlin.wiwiss.d2rq.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.optimizer.VarFinder;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Produces {@link Binding}s from {@link ResultRow}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: BindingMaker.java,v 1.3 2009/02/09 12:21:30 fatorange Exp $
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
	
	/**
	 * Additional method that makes from a result-row binding a binding.
	 * The difference to the method above (makeBinding) is that optional
	 * bindings are regarded. 
	 * This means if a binding has no value, it will NOT be rejected. 
	 *	
	 * @param row - Result row with the data from the database
	 * @return Binding - the created binding.
	 */
	public Binding makeOptionalBinding(ResultRow row, VarFinder varFinder) 
	{
		BindingMap result = new BindingMap();
		Iterator it = variableNamesToNodeMakers.keySet().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			NodeMaker nodeMaker = (NodeMaker) variableNamesToNodeMakers.get(variableName);
			Node node = nodeMaker.makeNode(row);
			if (node != null) 
			{
				result.add(Var.alloc(variableName), node);
			}else if (varFinder.getFixed().contains(Var.alloc(variableName)))
			{
				// If the node has the value null and the variable for this node is no
				// optional variable, then reject the complete binding, because this
				// variable is not bound
				// Only optional variables with null values are accepted in the binding
				return null;
			}
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
		return result.toString();
	}
}
