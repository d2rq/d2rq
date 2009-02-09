package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.LockNone;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * A DatasetGraph that has a single GraphD2RQ as its default
 * graph and no named graphs.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RQDatasetGraph.java,v 1.2 2009/02/09 12:21:30 fatorange Exp $
 */
public class D2RQDatasetGraph implements DatasetGraph {
	private final static Lock LOCK_INSTANCE = new LockNone();
	
	private final GraphD2RQ graph;
	
	public D2RQDatasetGraph(GraphD2RQ graph) {
		this.graph = graph;
	}
	
	public boolean containsGraph(Node graphNode) {
		return false;
	}

	public Graph getDefaultGraph() {
		return graph;
	}

	public Graph getGraph(Node graphNode) {
		return null;
	}

	public Lock getLock() {
		return LOCK_INSTANCE;
	}

	public Iterator listGraphNodes() {
		return NullIterator.instance();
	}

	public int size() {
		return 0;	// Just default graph
	}

	public void close() {
		graph.close();
	}
}
