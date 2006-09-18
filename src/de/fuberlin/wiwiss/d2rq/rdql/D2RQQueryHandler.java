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
 * A D2RQQueryHandler handles queries on behalf of a {@link GraphD2RQ}.
 * 
 * @author jgarbers
 * @version $Id: D2RQQueryHandler.java,v 1.8 2006/09/18 16:59:25 cyganiak Exp $
 */
public class D2RQQueryHandler extends SimpleQueryHandler implements QueryHandler {
	private GraphD2RQ graph;

	public D2RQQueryHandler(Graph graph) {
		super(graph);
		this.graph = (GraphD2RQ) graph;
	}     

	public Stage patternStage(Mapping map, ExpressionSet constraints, Triple [] t) { 
		return new D2RQPatternStage(this.graph, map, constraints, t);
	}
}
