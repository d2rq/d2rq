/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;

import de.fuberlin.wiwiss.d2rq.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.TripleQuery;
import de.fuberlin.wiwiss.d2rq.helpers.VariableIndex;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;

/** 
 * Handles variable node constraints for a TripleQuery conjunction.
 * Assumption: Bound variables in conjunction allready have been bound.
 * This code could as well be kept in PatternQueryCombiner.
 * 
 * @author jgarbers
 *
 */
class ConstraintHandler {
    public boolean possible=true;
    public VariableBindings bindings;
    TripleQuery[] conjunction;
    /** Mapping between a variable (Node) and its NodeConstraints. */
    public Map variableToConstraint=new HashMap(); 
    ExpressionSet rdqlConstraints;
    ExpressionTranslator rdqlTranslator;
    
    public void setVariableBindings(VariableBindings bindings) {
        this.bindings=bindings;
    }
    
    public void setTripleQueryConjunction(TripleQuery[] conjunction) {
        this.conjunction=conjunction;
    }
    
    public void setRDQLConstraints(ExpressionSet rdqlConstraints) {
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
            NodeConstraint c=new NodeConstraint();
            do {
                c.infoAdded = false;
                NodeMakerIterator e=makeNodeMakerIterator(varIndices);
                 while (possible && e.hasNext()) {
                    NodeMaker n = e.nextNodeMaker();
                    n.matchConstraint(c);
                    possible = c.possible;
                }
            } while (possible && c.infoAdded);
            variableToConstraint.put(node,c);
        }
    }
        
    /**
     * Creates SQL code for the node constraints.
     * @param sql contains both the places where to store expressions 
     * and the methods, how to format them.
     */
    public void addConstraintsToSQL(SQLStatementMaker sql) {
        Iterator it=variableToConstraint.values().iterator();
        while (it.hasNext()) {
            NodeConstraint c=(NodeConstraint)it.next();
            c.addConstraintsToSQL(sql);
        }
        // TODO addRDQLConstraints(sql);
    }
    
    void addRDQLConstraints(SQLStatementMaker sql) {
        if (rdqlTranslator==null)
            rdqlTranslator=new ExpressionTranslator(this);
        Set s=new HashSet();
        Iterator it=rdqlConstraints.iterator();
        while (possible && it.hasNext()) {
            Expression e=(Expression)it.next();
            String str=rdqlTranslator.translateToString(e);
            if (str!=null)
                s.add(str);
        }
        sql.addConditions(s);
    }
    
 /*   String rdqlConstraint(SQLStatementMaker sql, Expression e) {
        StringBuffer b=rdqlTranslator.translate(e);
        
        // default: do nothing
    }
    */
    // TODO other cases ;-)
    // see RDQLExpressionTranslator
    
    public NodeMakerIterator makeNodeMakerIterator(Set indexSet)  {
        return new NodeMakerIterator(conjunction,indexSet);
    }
    
	public class NodeMakerIterator implements Iterator {
	    TripleQuery[] conjunction;
	    Iterator indexSetIterator;
		public NodeMakerIterator(TripleQuery[] conjunction, Set indexSet) {
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
            NodeMaker n = conjunction[i.tripleNr]
                    .getNodeMaker(i.nodeNr);
            return n;
        }
        public Object next() {
            return nextNodeMaker();
        }
	}

}


