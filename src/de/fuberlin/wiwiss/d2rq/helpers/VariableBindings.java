/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.helpers;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Domain;

/** 
 * A class for capturing binding information for variables that occour in a
 * {@link CombinedPatternStage}.
 * 
 * @author jgarbers
 *
 */
public class VariableBindings {
    
    /**
     * variable bindings from inputDomain
     */
    public Domain inputDomain;

    // protected Set boundVariables=new HashSet(); // set of variable Nodes that
	// are to be bound in this or the previous stage
    
    /** 
     * variable Nodes in given triples
     */
	public Map variableNameToNodeMap = new HashMap(); 

	/**
	 * variables nodes bound in a previous stage.
	 * variableIndex -> set of VariableIndex.
	 * Optimized for variable lookup in RDQL conditions.
	 * @see #bindVariableToShared
	 */
	public Map boundVariableToShared = new HashMap(); 

	/** 
	 * variable positions bound in previous stages. 
	 * Domain index (Integer) -> set of VariableIndex.
	 * => update these nodes in actual Triple[].
	 */
	public Map boundDomainIndexToShared = new HashMap(); 
	
	/**	 
	 * variables nodes to be bound in this stage.
	 * variable (node) -> set of VariableIndex.
	 * For each variable a set of occurence positions.
	 * Optimized for variable name lookup.
	 */
	public Map bindVariableToShared = new HashMap(); 

	/**
	 * variables nodes to be bound in this stage.
	 * variableIndex -> set of VariableIndex.
	 * Optimized for variable index lookup.
	 * @see #bindVariableToShared
	 */
	public Map bindVariableIndexToShared = new HashMap(); 

	/**
	 * set of shared variable Nodes that are to be bound in this stage.
	 */
	public Set sharedBindVariables = new HashSet(); 

	/**
	 * set of indices of shared variables in a query.
	 */
	public Set sharedBindIndices = new HashSet(); 
	
	
	protected void addVariable(Node n) {
	    String key=n.getName();
	    variableNameToNodeMap.put(key,n);
	}
	
	public boolean isBound(String var) {
	    Node n=(Node)variableNameToNodeMap.get(var);
	    return (n!=null) && isBound(n);
	}
	public boolean isBound(Node var) {
	    return boundVariableToShared.get(var)!=null;
	}
	public boolean isBound(VariableIndex var) {
	    return boundDomainIndexToShared.get(var)!=null;
	}
	public boolean isBind(String var) {
	    Node n=(Node)variableNameToNodeMap.get(var);
	    return (n!=null) && isBind(n);
	}
	public boolean isBind(Node var) {
	    return bindVariableToShared.get(var)!=null;
	}
	public boolean isBind(VariableIndex var) {
	    return bindVariableIndexToShared.get(var)!=null;
	}
	
	/** 
	 * Looks up <code>key</code> in a object to varIndexSet map, adds
	 * <code>varIndexMember</code> to the set or creates it.
	 * Helper method.
	 * @param map one of: boundVariableToShared, boundDomainIndexToShared, bindVariableToShared, bindVariableIndexToShared
	 * @param key
	 * @param varIndexMember
	 * @return the (newly created) varIndexSet as a convenience
	 */
	protected Set mapToSharedPut(Map map, Object key, VariableIndex varIndexMember) {
		Set varIndexSet=(Set) map.get(key);
		if (varIndexSet==null) {
		   varIndexSet=new HashSet();
		   map.put(key,varIndexSet);
		}
		varIndexSet.add(varIndexMember);
		return varIndexSet;
	}
	
	/** 
	 * Adds information for a variable that is to be bound in this stage.
	 * 
	 * @param node the variable name
	 * @param domainIndex the variable index in Domain
	 * @param tripleNr the node's triple position
	 * @param nodeNr the node's position within the triple
	 */
	public void addBindNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		addVariable(node);
		VariableIndex varIndex=new VariableIndex(tripleNr,nodeNr);		
		Set varIndexSet=mapToSharedPut(bindVariableToShared, node, varIndex);
		bindVariableIndexToShared.put(varIndex,varIndexSet);
	}
	
	/** 
	 * Adds information for a node that was already bound in this or a previous stage.
	 * 
	 * @param node the variable name
	 * @param domainIndex the variable index in Domain
	 * @param tripleNr the node's triple position
	 * @param nodeNr the node's position within the triple
	 */
	public void addBoundNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		addVariable(node);
		VariableIndex varIndex=new VariableIndex(tripleNr,nodeNr);
		Set varIndexSet=(Set)bindVariableToShared.get(node);
		if (varIndexSet==null) {	
			Integer domainIndexObject=new Integer(domainIndex);
			varIndexSet=mapToSharedPut(boundVariableToShared,domainIndexObject,varIndex);
			boundDomainIndexToShared.put(domainIndexObject,varIndexSet);
			// mapToSharedPut(boundDomainIndexToShared,domainIndexObject,varIndex);
		} else {
			sharedBindVariables.add(node);
			varIndexSet.add(varIndex);
			sharedBindIndices.add(varIndex);
			bindVariableIndexToShared.put(varIndex,varIndexSet);
		}
	}
	
	/*
	public Map variableNameToNodeMap() {
	    Map m=new HashMap();
	    Iterator it=variables.iterator();
	    while (it.hasNext()) {
	        Node n=(Node)it.next();
	        String name=n.getName();
	        m.put(name,n);
	    }
	    return m;
	}
	*/
	
	// currently not used
	private static void triplesFindVariablesAndShared(Triple[] triples, Collection variables, Collection sharedVariables) {
		for (int i=0; i<triples.length; i++) {
			Triple t=triples[i];
			for (int j=0; j<3; j++) {
				Node n=null;
				switch (j) {
				case 0: n=t.getSubject(); break;
				case 1: n=t.getPredicate(); break;
				case 2: n=t.getObject(); break;
				}
				if (n.isVariable()) {
					if (variables.contains(n)) {
						sharedVariables.add(n);
					} else {
						variables.add(n);
					}
				}
			}
		}
	}


}

