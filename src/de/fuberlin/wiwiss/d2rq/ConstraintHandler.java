package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.helpers.VariableBindings;
import de.fuberlin.wiwiss.d2rq.helpers.VariableIndex;

class ConstraintHandler {
    public boolean possible=true;
    VariableBindings bindings;
    TripleQuery[] conjunction;
    Map variableToConstraint=new HashMap();
    
    public void setVariableBindings(VariableBindings bindings) {
        this.bindings=bindings;
    }
    
    public void setTripleQueryConjunction(TripleQuery[] conjunction) {
        this.conjunction=conjunction;
    }
    public void makeConstraints() {
        Iterator it=bindings.sharedBindVariables.iterator();
        while (possible && it.hasNext()) {
            Object node=it.next();
            Set varIndices=(Set) bindings.bindVariableToShared.get(node);
            NodeConstraint c=new NodeConstraint();
            do {
                c.infoAdded = false;
                Iterator e = varIndices.iterator();
                while (possible && e.hasNext()) {
                    VariableIndex i = (VariableIndex) e.next();
                    NodeMaker n = conjunction[i.tripleNr]
                            .getNodeMaker(i.nodeNr);
                    n.matchConstraint(c);
                    possible = c.possible;
                }
            } while (c.infoAdded);
        }
    }
    
    
    public void addConstraintsToSQL(SQLStatementMaker sql) {
      // TODO   
    }
    
}


