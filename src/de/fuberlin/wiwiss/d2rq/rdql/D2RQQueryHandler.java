package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Collection;

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
 * @version $Id: D2RQQueryHandler.java,v 1.10 2006/10/16 12:46:01 cyganiak Exp $
 */
public class D2RQQueryHandler extends SimpleQueryHandler implements QueryHandler {
	private Collection rdfRelations;

	public D2RQQueryHandler(Graph graph, Collection rdfRelations) {
		super(graph);
		this.rdfRelations = rdfRelations;
	}     

	public Stage patternStage(Mapping map, ExpressionSet constraints, Triple [] t) { 
		return new D2RQPatternStage(this.rdfRelations, map, constraints, t);
	}
}