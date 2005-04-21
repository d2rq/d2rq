/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;


import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.Stage;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;

/**
a D2RQQueryHandler handles queries on behalf of a GraphD2RQ graph. 
subclassing from SimpleQueryHandler for convenience, makes differences clear.
**/

public class D2RQQueryHandler extends SimpleQueryHandler implements QueryHandler {
	private GraphD2RQ graph; // see also SimpleQueryHandler.graph
	private boolean doFastpath;  // if true, enable fastpath optimization

    public D2RQQueryHandler( GraphD2RQ graph ) {
        super((Graph)graph);
		this.graph = graph;
		doFastpath = true;
    }     
   
    public static boolean runVersion2=true;
    
    public Stage patternStage( Mapping map, ExpressionSet constraints, Triple [] t )
    { 
    		if (runVersion2) {
    		    D2RQPatternStage2 inst= new D2RQPatternStage2( graph, map, constraints, t );
    			inst.setup();
    			return inst;
    		} else 
    			return new D2RQPatternStage( graph, map, constraints, t ); // jg see PatternStage for overriding
    }
}
