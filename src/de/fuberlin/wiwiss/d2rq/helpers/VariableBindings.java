package de.fuberlin.wiwiss.d2rq.helpers;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

// reference: CombinedPatternStage

public class VariableBindings {
	// protected Set boundVariables=new HashSet(); // set of variable Nodes that
	// are to be bound in this or the previous stage
	public Set variables = new HashSet(); // variable Nodes in given triples

	// variable positions bound in previous stages => update these nodes in actual Triple[]
	public Map boundDomainIndexToShared = new HashMap(); // Domain index (Integer) -> VariableIndex
	
	//	 variables nodes to be bound in this stage
	//	 for each variable a set of occurence positions:
	public Map bindVariableToShared = new HashMap(); // variable (node) -> set
													 // of VariableIndex

	public Map bindVariableIndexToShared = new HashMap(); // variableIndex ->
														  // set of
														  // VariableIndex

	public Set sharedBindVariables = new HashSet(); // set of shared variable
													// Nodes that are to be
													// bound in this

	public Set sharedBindIndices = new HashSet(); // set of their indices
	
	// returns the (newly created) varIndexSet as a convenience
	public Set mapToSharedPut(Map map, Object key, VariableIndex varIndexMember) {
		Set varIndexSet=(Set) map.get(key);
		if (varIndexSet==null) {
		   varIndexSet=new HashSet();
		   map.put(key,varIndexSet);
		}
		varIndexSet.add(varIndexMember);
		return varIndexSet;
	}
	
	public void addBindNode(Object X, int domainIndex, int tripleNr, int nodeNr) {
		variables.add(X);
		VariableIndex varIndex=new VariableIndex(tripleNr,nodeNr);		
		Set varIndexSet=mapToSharedPut(bindVariableToShared, X, varIndex);
		bindVariableIndexToShared.put(varIndex,varIndexSet);
	}
	
	// X is a Node
	public void addBoundNode(Object X, int domainIndex, int tripleNr, int nodeNr) {
		variables.add(X);
		VariableIndex varIndex=new VariableIndex(tripleNr,nodeNr);
		Set varIndexSet=(Set)bindVariableToShared.get(X);
		if (varIndexSet==null) {	
			Integer domainIndexObject=new Integer(domainIndex);
			mapToSharedPut(boundDomainIndexToShared,domainIndexObject,varIndex);
		} else {
			sharedBindVariables.add(X);
			varIndexSet.add(varIndex);
			sharedBindIndices.add(varIndex);
			bindVariableIndexToShared.put(varIndex,varIndexSet);
		}
	}
	
	// currently not used
	public static void triplesFindVariablesAndShared(Triple[] triples, Collection variables, Collection sharedVariables) {
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

