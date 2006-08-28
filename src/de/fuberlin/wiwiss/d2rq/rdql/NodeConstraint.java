/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.List;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.LiteralMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * TODO: Describe this type
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeConstraint.java,v 1.6 2006/08/28 21:13:47 cyganiak Exp $
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
     * We see a literal NodeMaker.
     * @param m
     */
    public void matchLiteralMaker(LiteralMaker m);

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
     * NodeMakers with an attached {@link ValueSource} call this.
     * @param c
     */
    public void matchColumn(Column c);

    public void matchPattern(Pattern p, List columns);

    public void matchBlankNodeIdentifier(
    		BlankNodeIdentifier id, List columns);
    
    public void addEqualColumn(Column c1, Column c2);
    
    public void addConstraintsToSQL(SelectStatementBuilder sql);
}