package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/** 
 * Handles variable node constraints for a TripleQuery conjunction.
 * Assumption: Bound variables in conjunction allready have been bound.
 * (This code could as well be kept in PatternQueryCombiner.)
 * 
 * @author jgarbers
 * @version $Id: ConstraintHandler.java,v 1.3 2007/11/15 15:54:51 cyganiak Exp $
 */
public class ConstraintHandler {
    public boolean possible=true;
    public VariableBindings bindings;
    RDFRelation[] conjunction;
    /** Mapping between a variable (Node) and its NodeConstraints. */
    public Map variableToConstraint=new HashMap(); 
    Collection rdqlConstraints;
    
    public void setVariableBindings(VariableBindings bindings) {
        this.bindings=bindings;
    }
    
    public void setTripleQueryConjunction(RDFRelation[] conjunction) {
        this.conjunction=conjunction;
    }
    
    public void setRDQLConstraints(Collection rdqlConstraints) {
        this.rdqlConstraints=rdqlConstraints;
    }
    
    /** 
     * Creates Node constraints for all shared Bind variables.
     * Iterates over the positions, where shared Bind variables occour
     * until no more constraint information is derived.
     */
    public void makeConstraints() {
        Iterator it=bindings.sharedBindVariables.iterator();
        while (possible && it.hasNext()) {
            Node node=(Node)it.next();
            Set varIndices=(Set) bindings.bindVariableToShared.get(node);
            NodeConstraintImpl c = new NodeConstraintImpl();
            do {
                c.resetInfoAdded();
                NodeMakerIterator e=makeNodeMakerIterator(varIndices);
                 while (possible && e.hasNext()) {
                    NodeMaker n = e.nextNodeMaker();
                    n.describeSelf(c);
                    possible = c.isPossible();
                }
            } while (possible && c.infoAdded());
            variableToConstraint.put(node,c);
        }
    }
        
    /**
     * Creates SQL code for the node constraints.
     * @param sql contains both the places where to store expressions 
     * and the methods, how to format them.
     */
    public void addConstraintsToSQL(SelectStatementBuilder sql) {
        Iterator it=variableToConstraint.values().iterator();
        while (it.hasNext()) {
            NodeConstraintImpl c=(NodeConstraintImpl)it.next();
            c.addConstraintsToSQL(sql);
        }
    }
       
    public NodeMakerIterator makeNodeMakerIterator(Set indexSet)  {
        return new NodeMakerIterator(conjunction,indexSet);
    }
    
    /**
     * Iterates over all the nodes of a triple conjunction.
     * @author jgarbers
     *
     */
	public class NodeMakerIterator implements Iterator {
	    RDFRelation[] conjunction;
	    Iterator indexSetIterator;
		public NodeMakerIterator(RDFRelation[] conjunction, Set indexSet) {
		    this.conjunction=conjunction;
		    this.indexSetIterator=indexSet.iterator();
		}
        public void remove() {
        }
        public boolean hasNext() {
            return indexSetIterator.hasNext();
        }
        public NodeMaker nextNodeMaker() {
            VariableIndex i = (VariableIndex) indexSetIterator.next();
	        return conjunction[i.tripleNr].nodeMaker(i.nodeNr);
        }
        public Object next() {
            return nextNodeMaker();
        }
	}

}


