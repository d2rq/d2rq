/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.graph.query.Stage;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * A D2RQQueryHandler handles queries on behalf of a GraphD2RQ graph. 
 * Subclassing from SimpleQueryHandler for convenience, makes differences clear.
 * 
 * @author jgarbers
 * @version $Id: D2RQQueryHandler.java,v 1.6 2006/08/28 20:23:42 cyganiak Exp $
 */
public class D2RQQueryHandler extends SimpleQueryHandler implements QueryHandler {
	private GraphD2RQ graph;

    public D2RQQueryHandler(Graph graph) {
        super(graph);
		this.graph = (GraphD2RQ) graph;
    }     
   
    public static final int runVersion=4;
    
    public Stage patternStage(Mapping map, ExpressionSet constraints, Triple [] t) { 
    	D2RQPatternStage result = new D2RQPatternStage(this.graph, map, constraints, t);
    	result.setup();
    	return result;
    }
}
