package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.List;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

/**
 * @author Richard Cyganiak
 * @version $Id: NodeConstraint.java,v 1.10 2006/09/11 23:02:49 cyganiak Exp $
 */
public interface NodeConstraint {

    // same as PlaceholderNode constants
    public static final int NotFixedNodeType = 0;
    public static final int BlankNodeType = 1;
    public static final int UriNodeType = 2;
    public static final int LiteralNodeType = 4;    
    
    public boolean isPossible();
 
    public void matchImpossible();
    
    /** 
     * We see a fixed NodeMaker.
     * @param node
     */
    public void matchFixedNode(Node node);
 
    /**
     * We see a NodeMaker, that produces nodes of type 
     * BlankNodeType, UriNodeType or LiteralNodeType. 
     * @param t
     */
    public void matchNodeType(int t);
    
    /** 
     * Constraints given on Nodes that are equal to Columns
     * can be directly translated to Column constraints.
     * NodeMakers with an attached {@link ValueMaker} call this.
     * @param c
     */
    public void matchColumn(Attribute c);

    public void matchPattern(Pattern p, List columns);

    public void matchBlankNodeIdentifier(
    		BlankNodeID id, List columns);
    
    public void addEqualColumn(Attribute c1, Attribute c2);
    
    public void addConstraintsToSQL(SelectStatementBuilder sql);

    /**
     * We see a NodeMaker that produces literals with the given language
     * and datatype (possibly null).
     */
	public void matchLiteralType(String language, RDFDatatype datatype);
}