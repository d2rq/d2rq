package de.fuberlin.wiwiss.d2rq.engine;

import com.hp.hpl.jena.sparql.core.DatasetGraphOne;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * A DatasetGraph that has a single GraphD2RQ as its default
 * graph and no named graphs.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQDatasetGraph extends DatasetGraphOne {
	
	public D2RQDatasetGraph(GraphD2RQ graph) {
		// This constructor is protected in DatasetGraphOne, so we
		// need to make our own class with public constructor
		super(graph);
	}
}
