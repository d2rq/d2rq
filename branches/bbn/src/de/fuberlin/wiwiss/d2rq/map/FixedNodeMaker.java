/*
 * $Id: FixedNodeMaker.java,v 1.1 2006/04/12 09:53:04 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.PlaceholderNode;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * NodeMaker that returns a fixed node.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class FixedNodeMaker implements NodeMaker {
	private Node fixedNode; // can be instance of PlaceholderNode

    public void matchConstraint(NodeConstraint c) {
        c.matchFixedNode(fixedNode);
    }

	public FixedNodeMaker(Node fixedNode) {
		this.fixedNode = fixedNode;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#isDisjointFromOtherNodeMakers()
	 */
	public boolean isURIPattern() {
		return false;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#couldFit(com.hp.hpl.jena.graph.Node)
	 */
	public boolean couldFit(Node node) {
		return node.isVariable() || this.fixedNode.equals(node);
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumns()
	 */
	public List getColumns() {
		return new ArrayList(0);
	}
    
    public void setColumns( List columns ){
        //This should never be set to anything. 
    }

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumnValues(com.hp.hpl.jena.graph.Node)
	 */
	public Map getColumnValues(Node node) {
		return new HashMap(0);
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getNode(java.lang.String[], java.util.Map)
	 */
	public Node getNode(String[] row, Map columnNameNumberMap) {
	    //	fixedNode can be instance of PlaceholderNode
		return PlaceholderNode.unwrapNode(this.fixedNode);
	}
    
    public Node getNode(){
        return fixedNode;
    }
}
