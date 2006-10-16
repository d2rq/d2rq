package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;


/**
 * @author jgarbers
 * @version $Id: VarInfos.java,v 1.3 2006/10/16 12:46:00 cyganiak Exp $
 */
public class VarInfos {
	private final int tripleCount;
	/**
	 * Variables that are bound at position i for the first time.
	 * Returns VariableIndex
	 */
	private final Map[] bindVariables;
	/**
	 * Variables from ExpressionSet that are bound when ith triple was matched.
	 * Note: boundVariables[i] contains also all Variables from boundVariables[i-1],
	 * etc. and from previous stages
	 */
	private Set[] boundVariables;

	/** Fast lookup information for different types of variables (shared, bind, bound). */
	public final VariableBindings allBindings;

	public final Collection allExpressions;

	public VarInfos(Mapping queryMapping, ExpressionSet queryConstraints, 
			int tripleCount) {
		this.tripleCount = tripleCount;
		Set constraintVariableNodes = new HashSet();
		List constraintVariableNodeSets = new ArrayList();
		addVariableNodes(queryConstraints,constraintVariableNodes,constraintVariableNodeSets);
		Set constraintVariableNodesFromInitialMapping = new HashSet();
		Iterator it=constraintVariableNodes.iterator();
		while (it.hasNext()) {
			Node var=(Node)it.next();
			if (queryMapping.hasBound(var)) {
				constraintVariableNodesFromInitialMapping.add(var);
			}
		}
		this.boundVariables = new Set[tripleCount];
		for (int i=0; i<tripleCount; i++) {
			this.boundVariables[i] = new HashSet(constraintVariableNodesFromInitialMapping);
		}
		this.bindVariables = new HashMap[tripleCount];
		this.allBindings = new VariableBindings();
		this.allExpressions = new ArrayList();
	}

	public Set boundVariables(int index) {
		return this.boundVariables[index];
	}
	
	public void addBindNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		if (bindVariables[tripleNr]==null)
			bindVariables[tripleNr]=new HashMap();
		bindVariables[tripleNr].put(node,new VariableIndex(tripleNr,nodeNr));
		updateBoundVariables(node,tripleNr);
		allBindings.addBindNode(node, domainIndex, tripleNr, nodeNr);
	}
	
	public void addBoundNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		allBindings.addBoundNode(node, domainIndex, tripleNr, nodeNr);
	}

	public void addExpression(Expression e, int tripleNr) {
        allExpressions.add(e);
	}
	
	private void updateBoundVariables(Node node, int tripleNr) {
		for (int i=tripleNr; i<tripleCount;i++)
			boundVariables[i].add(node);
	}

	private void addVariableNodes(ExpressionSet s, Collection nodes, List listOfNodes) {
		Set names=null;
		if (nodes!=null)
			names=new HashSet();
		List namesList=null;
		if (listOfNodes!=null) {
			namesList=new ArrayList();
		}
		Iterator it=s.iterator();
		while (it.hasNext()) {
			Expression e=(Expression)it.next();
			Set s2;
			if (namesList!=null) {
				s2=new HashSet();
			} else {
				s2=names;
			}
			Expression.Util.addVariablesOf( s2, e );
			if (names!=null && s2!=names) {
				names.addAll(s2);
			}
		}
		if (nodes!=null)
			namesToNodes(names,nodes);
		if (listOfNodes!=null) {
			it=namesList.iterator();
			while (it.hasNext()) {
				Collection in=(Collection)it.next();
				Collection out=new HashSet();
				namesToNodes(in,out);
				listOfNodes.add(out);
			}
		}
	}

	private void namesToNodes(Collection names,Collection nodes) {
		Iterator it=names.iterator();
		while (it.hasNext()) {
			String name=(String)it.next();
			Node node=Node.createVariable( name );
			nodes.add(node);
		}    	  
	}
}
