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
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;


/**
 * @author jgarbers
 * @version $Id: VarInfos.java,v 1.1 2006/09/18 16:59:26 cyganiak Exp $
 */
public class VarInfos {
    /**
     * variable bindings from inputDomain
     */
//    private Domain inputDomain;
	
	protected int tripleCount;
	/**
	 * All variables from ExpressionSet.
	 */
	protected Set constraintVariableNodes;
	protected List constraintVariableNodeSets;
	/**
	 * Variables from ExpressionSet that are bound by previous stage.
	 */
	protected Set constraintVariableNodesFromInitialMapping;

	/**
	 * Variables that are bound at position i for the first time.
	 * Returns VariableIndex
	 */
	protected Map[] bindVariables;
	
	/**
	 * Variables from ExpressionSet that are bound when ith triple was matched.
	 * Note: boundVariables[i] contains also all Variables from boundVariables[i-1],
	 * etc. and from previous stages
	 */
	protected Set[] boundVariables;
	
	/** Fast lookup information for different types of variables (shared, bind, bound). */
    public VariableBindings allBindings=new VariableBindings();
    public VariableBindings[][] partBindings; // partBindings[i][j]: starting from i, ending in j
    protected boolean partBindingVariableIndicesArePartRelative=true;
    
    public Collection allExpressions=new ArrayList();
    public Collection[][] partExpressions;

	public VarInfos(Mapping queryMapping, ExpressionSet queryConstraints, int tripleCount) {
		this.tripleCount=tripleCount;
		constraintVariableNodes=new HashSet();
		constraintVariableNodeSets=new ArrayList();
		addVariableNodes(queryConstraints,constraintVariableNodes,constraintVariableNodeSets);
		constraintVariableNodesFromInitialMapping=new HashSet();
		Iterator it=constraintVariableNodes.iterator();
		while (it.hasNext()) {
			Node var=(Node)it.next();
			if (queryMapping.hasBound(var)) {
				constraintVariableNodesFromInitialMapping.add(var);
			}
		}
		boundVariables=new Set[tripleCount];
		for (int i=0; i<tripleCount; i++) {
			boundVariables[i]=new HashSet(constraintVariableNodesFromInitialMapping);
		}
		bindVariables=new HashMap[tripleCount];
	}

	public void addBindNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		if (bindVariables[tripleNr]==null)
			bindVariables[tripleNr]=new HashMap();
		bindVariables[tripleNr].put(node,new VariableIndex(tripleNr,nodeNr));
		updateBoundVariables(node,tripleNr);
		if (allBindings!=null)
			allBindings.addBindNode(node, domainIndex, tripleNr, nodeNr);
		if (partBindings!=null)
			addPartBindings(node, domainIndex, tripleNr, nodeNr,true);
	}
	public void addBoundNode(Node node, int domainIndex, int tripleNr, int nodeNr) {
		if (allBindings!=null)
			allBindings.addBoundNode(node, domainIndex, tripleNr, nodeNr);
		if (partBindings!=null)
			addPartBindings(node, domainIndex, tripleNr, nodeNr,false);
	}
	public void addExpression(Expression e, int tripleNr) {
	    if (allExpressions!=null)
	        allExpressions.add(e);
	    if (partExpressions!=null)
	        addPartExpressions(e,tripleNr);
	}

	public void allocParts() {
		partBindings=new VariableBindings[tripleCount][tripleCount];
		partExpressions=new Collection[tripleCount][tripleCount];
		for (int i=0; i<=tripleCount; i++) 
			for (int j=i; j<tripleCount; j++) {
				partBindings[i][j]=new VariableBindings();
				partExpressions[i][j]=new ArrayList();
			}
	}
	
	public void addPartExpressions(Expression e, int tripleNr) {
		// tripleNr must be in range i..j
		for (int i=0; i<=tripleNr; i++) {
			for (int j=tripleNr; j<tripleCount; j++) {
			    partExpressions[i][j].add(e);
			}
		}
	}
	public void addPartBindings(Node node, int domainIndex, int tripleNr, int nodeNr, boolean isBind) {
		// tripleNr must be in range i..j
		for (int i=0; i<=tripleNr; i++) {
			int relative=partBindingVariableIndicesArePartRelative?(tripleNr-i):i;
			for (int j=tripleNr; j<tripleCount; j++) {
				if (isBind)
					partBindings[i][j].addBindNode(node,domainIndex,relative,nodeNr);				
				else 
					partBindings[i][j].addBoundNode(node,domainIndex,relative,nodeNr);
			}
		}	
	}
	protected void updateBoundVariables(Node node, int tripleNr) {
		for (int i=tripleNr; i<tripleCount;i++)
			boundVariables[i].add(node);
	}

	
	public void setInputDomain(Domain d) {
// Was never read, so I commented it out -- RC
//		inputDomain=d;
		// TODO add to variableBindings?
	}

	public Set getBindVariables(int index, boolean nullIfNone) {
		if (bindVariables[index]!=null)
			return bindVariables[index].entrySet();
		else if (nullIfNone)
			return null;
		else
			return new HashSet();
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
