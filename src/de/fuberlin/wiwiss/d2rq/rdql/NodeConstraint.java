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

import de.fuberlin.wiwiss.d2rq.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.LiteralMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.RegexRestriction;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;

/**
 * Holds constraint information for a variable node.
 * In a query a variable node is either a result value, or a shared variable
 * or both. If it is a shared variable, we collect into a NodeConstraint 
 * all constraining information that we know about the different node positions
 * from the {@link PropertyBridge}s.
 * 
 * @author jg
 * @since V0.3
 */
public class NodeConstraint {
	/** true means: satisfiable. */
    public boolean possible=true;
    /** a flag that shows, if constraint information was added. */
    boolean infoAdded = false;

    /** if not null, we allready know the value of the variable, but still have to check for 
     * other occourences. */
    public Node fixedNode;
    /** What is the type, an URI, a blank node or a literal? */
    public int nodeType=NotFixedNodeType;
    /** A literalMaker can be matched against another literalMaker. 
     * Both literals must produce the same language or XSD-Type. */
    public LiteralMaker literalMaker; // used by LiteralMaker only

    // ValueSource constraints
    /** valueSource condition Strings (SQL) */
    protected Set conditions=new HashSet(); 
    /** set of columns that are equal with this node */
    protected Set columns=new HashSet(); 
    /** all patterns to be matched against */
    protected Set patterns=new HashSet(); 
    // protected Map columnToEqualColumns=new HashMap(); // column -> set of equal columns (may be inferred by patterns)
    // protected Map columnToDataType=new HashMap(); // column -> Database.ColumnType (Integer)
    // protected Map patternToEqualPatterns=new HashMap(); // pattern 
    // protected Set regexSet=new HashSet();
    // protected int maxLength=-1;
 
    public static final int NotFixedNodeType = 0;
    public static final int BlankNodeType = 1;
    public static final int UriNodeType = 2;
    public static final int LiteralNodeType = 3;    
    
    /** 
     * We see a literal NodeMaker.
     * @param m
     */
    public void matchLiteralMaker(LiteralMaker m) {
        if (!possible)
            return;
        if (literalMaker == null) {
            literalMaker=m;
        } else if (possible){
            possible=m.matchesOtherLiteralMaker(literalMaker);
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
    public void matchValueSource(Column c) {
        if (!possible)
            return;
        columns.add(c);
    }
    
    /** 
     * Pattern-Constraints can be translated to column constraints.
     * NodeMakers with an attached {@link ValueSource} call this.
     * @param p
     */
    public void matchValueSource(Pattern p) {
        if (!possible)
            return;
        if (patterns!=null) {
            Iterator it = patterns.iterator();
            while (it.hasNext()) {
                Pattern prev=(Pattern)it.next();
                prev.matchPatternIntoNodeConstraint(p,this);
            }
        } else {
            patterns=new HashSet();
        }
        patterns.add(p);
        // we would like to have (expr = expr)
        // where expr is a concatenation.
        // Some SQL dialects seem to support "||" or infix "CONCAT" operator
        // MSAccess does not.
    }
    public void matchValueSource(RegexRestriction r) {
        if (!possible)
            return;
        // regex patterns different in different implementations of SQL
        // e.g. sqlite: "%" -> character sequence, "_" -> single char
        // e.g. MSAccess: "*" -> character sequence, "?" -> single char
    }
    public void matchValueSource(ValueSource s) {
        // skip any other
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
    public void conditionsAddEqual(String n1, String n2) {
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
    
    /** 
     * The collected constraints are created as SQL constraints.
     * @param sql the statment maker that gets the constraints. 
     * It knows about {@link Alias}es and correct quoting of values for integer/string
     * database columns.
     */
    public void addConstraintsToSQL(SQLStatementMaker sql) {
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
        sql.addConditions(conditions);
    }
}