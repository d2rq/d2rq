package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

public class NodeConstraint {
    public boolean possible=true;
    boolean infoAdded = false;

    public Node fixedNode;
    public int nodeType=NotFixedNodeType;
    public LiteralMaker literalMaker; // used by LiteralMaker only

    // ValueSource constraints
    protected Set conditions=new HashSet(); // valueSource condition Strings (SQL)
    protected Set columns=new HashSet(); // set of columns that equal with this node
    protected Set patterns=new HashSet(); // all patterns to be matched against
    // protected Map columnToEqualColumns=new HashMap(); // column -> set of equal columns (may be inferred by patterns)
    // protected Map columnToDataType=new HashMap(); // column -> Database.ColumnType (Integer)
    // protected Map patternToEqualPatterns=new HashMap(); // pattern 
    // protected Set regexSet=new HashSet();
    // protected int maxLength=-1;
 
    public static final int NotFixedNodeType = 0;
    public static final int BlankNodeType = 1;
    public static final int UriNodeType = 2;
    public static final int LiteralNodeType = 3;    
    
    public void matchLiteralMaker(LiteralMaker m) {
        if (!possible)
            return;
        if (literalMaker == null) {
            literalMaker=m;
        } else if (possible){
            possible=m.matchesOtherLiteralMaker(literalMaker);
        }
    }

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
    
    public void matchValueSource(Column c) {
        if (!possible)
            return;
        columns.add(c);
    }
    
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