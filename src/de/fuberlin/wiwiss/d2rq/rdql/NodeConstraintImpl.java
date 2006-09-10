package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * Holds constraint information for a variable node.
 * In a query a variable node is either a result value, or a shared variable
 * or both. If it is a shared variable, we collect into a NodeConstraint 
 * all constraining information that we know about the different node positions
 * from the {@link PropertyBridge}s.
 * 
 * @author jg
 * @version $Id: NodeConstraintImpl.java,v 1.5 2006/09/10 22:18:44 cyganiak Exp $
 */
public class NodeConstraintImpl implements NodeConstraint {
	/** true means: satisfiable. */
    private boolean possible=true;
    /** a flag that shows, if constraint information was added. */
    private boolean infoAdded = false;

    /** if not null, we allready know the value of the variable, but still have to check for 
     * other occourences. */
    private Node fixedNode;
    /** What is the type, an URI, a blank node or a literal? */
    private int nodeType=NotFixedNodeType;
    private String literalLanguage = null;
    private RDFDatatype literalDatatype = null;

    // ValueSource constraints
    /** valueSource condition Strings (SQL) */
    private Set conditions=new HashSet(); 
    /** set of columns that are equal with this node */
    private Set columns=new HashSet(); 
    /** all patterns to be matched against */
    private List patternConstraints = new ArrayList(); 
    // protected Map columnToEqualColumns=new HashMap(); // column -> set of equal columns (may be inferred by patterns)
    // protected Map columnToDataType=new HashMap(); // column -> Database.ColumnType (Integer)
    // protected Map patternToEqualPatterns=new HashMap(); // pattern 
    // protected Set regexSet=new HashSet();
    // protected int maxLength=-1;
 
    public boolean isPossible() {
    	return this.possible;
    }
    
    public void matchImpossible() {
    	this.possible = false;
    }
    
    public void resetInfoAdded() {
    	this.infoAdded = false;
    }
    
    public boolean infoAdded() {
    	return this.infoAdded;
    }
    
    public Node fixedNode() {
    	return this.fixedNode;
    }
    
    public Set patterns() {
    	Set result = new HashSet();
    	Iterator it = this.patternConstraints.iterator();
    	while (it.hasNext()) {
			PatternConstraint constraint = (PatternConstraint) it.next();
			result.add(constraint.pattern());
		}
    	return result;
    }
    
    public Set columns() {
    	return this.columns;
    }
    
	public void matchLiteralType(String language, RDFDatatype datatype) {
		if (!this.possible) return;
		if (this.nodeType == NotFixedNodeType) {
			matchNodeType(LiteralNodeType);
			this.literalLanguage = language;
			this.literalDatatype = datatype;
			return;
		}
		if (this.nodeType != LiteralNodeType
				|| (this.literalLanguage == null && language != null)
				|| (this.literalLanguage != null && !this.literalLanguage.equals(language))
				|| (this.literalDatatype == null && datatype != null)
				|| (this.literalDatatype != null && !this.literalDatatype.equals(datatype))) {
			matchImpossible();
		}
	}
	
    /** 
     * We see a fixed NodeMaker.
     * @param node
     */
    public void matchFixedNode(Node node) {
        if (!possible)
            return;
       if (fixedNode == null) {
            fixedNode = node;
            infoAdded = true;
            if (node.isLiteral())
                matchNodeType(LiteralNodeType);
            return;
        }
        possible = fixedNode.equals(node);
    }
 
    /**
     * We see a NodeMaker, that produces nodes of type 
     * BlankNodeType, UriNodeType or LiteralNodeType. 
     * @param t
     */
    public void matchNodeType(int t) {
        if (!possible)
            return;
       if (nodeType == NotFixedNodeType) {
            nodeType = t;
            infoAdded = true;
            return;
        }
        possible = (nodeType == t);
    }
    
    /** 
     * Constraints given on Nodes that are equal to Columns
     * can be directly translated to Column constraints.
     * NodeMakers with an attached {@link ValueSource} call this.
     * @param c
     */
    public void matchColumn(Column c) {
        if (!possible)
            return;
    	if (!this.patternConstraints.isEmpty()) {
    		matchImpossible();
    	}
        columns.add(c);
    }
        
    /** 
     * Pattern-Constraints can be translated to column constraints.
     * NodeMakers with an attached {@link ValueSource} call this.
     * @param p
     */
    public void matchPattern(Pattern p, List columns) {
    	if (!isPossible()) {
    		return;
    	}
    	if (!this.columns.isEmpty()) {
    		matchImpossible();
    	}
        Iterator it = this.patternConstraints.iterator();
        while (it.hasNext()) {
            PatternConstraint constraint = (PatternConstraint) it.next();
            constraint.match(p, columns);
        }
        this.patternConstraints.add(new PatternConstraint(p, columns));
    }

    private class PatternConstraint {
    	private Pattern pattern;
    	private List columns;
    	PatternConstraint(Pattern pattern, List columns) {
    		this.pattern = pattern;
    		this.columns = columns;
    	}
    	Pattern pattern() {
    		return this.pattern;
    	}
    	void match(Pattern p, List otherColumns) {
    		if (!this.pattern.equals(p)) {
    			matchImpossible();
    		}
    		if (this.columns.size() != otherColumns.size()) {
    			matchImpossible();
    		}
    		for (int i = 0; i < this.columns.size(); i++) {
    			Column col1 = (Column) this.columns.get(i);
    			Column col2 = (Column) otherColumns.get(i);
    			addEqualColumn(col1, col2);
    		}
    	}
    }
    
    public void addEqualColumn(Column c1, Column c2) {
        conditionsAddEqual(c1.getQualifiedName(),c2.getQualifiedName());
    }
    
    /** 
     * Adds a textual equivalence condition to <code>conditions</code>.
     * We avoid adding both "x=y" and "y=x" by sorting the arguments <code>n1</code> and <code>n2</code> first.
     * @param n1
     * @param n2
     */
    private void conditionsAddEqual(String n1, String n2) {
        int cmp=n1.compareTo(n2);
        if (cmp==0)
            return;
        else if (cmp==1){
            String n3=n1;
            n1=n2;
            n2=n3;
        }
        conditions.add(n1 + "=" + n2);
    }

    public void matchBlankNodeIdentifier(BlankNodeIdentifier id, List columns) {
    	// TODO Handle blank node IDs
    }

    /** 
     * The collected constraints are created as SQL constraints.
     * @param sql the statment maker that gets the constraints. 
     * It knows about Alias and correct quoting of values for integer/string
     * database columns.
     */
    public void addConstraintsToSQL(SelectStatementBuilder sql) {
        String value=null;
        String firstCol=null;
        if (fixedNode!=null)
            value=fixedNode.toString(); // TODO what is a clean way to extract uri or literal value?
        Iterator it=columns.iterator();
        while (it.hasNext()) {
            Column col=(Column)it.next();
            String colString=col.getQualifiedName();
            if (value==null) {
                if (firstCol==null) {
                    firstCol=colString;
                } else {
                    conditionsAddEqual(firstCol,colString);
                }
            } else {
                sql.addColumnValue(col,value);
            }
        }
        it = this.conditions.iterator();
        while (it.hasNext()) {
			String condition = (String) it.next();
			sql.addCondition(new Expression(condition));
		}
    }
}