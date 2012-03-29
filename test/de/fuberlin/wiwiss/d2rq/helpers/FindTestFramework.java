package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class FindTestFramework extends TestCase {
    protected static final Model m = ModelFactory.createDefaultModel();

    private GraphD2RQ graph;
	private Set<Triple> resultTriples; 

	protected void setUp() throws Exception {
		this.graph = (GraphD2RQ) new ModelD2RQ(D2RQTestSuite.ISWC_MAP, "TURTLE", "http://test/").getGraph();
	}

	protected void tearDown() throws Exception {
		this.graph.close();
	}

	protected void find(RDFNode s, RDFNode p, RDFNode o) {
		this.resultTriples = new HashSet<Triple>();
		ExtendedIterator<Triple> it = this.graph.find(toNode(s), toNode(p), toNode(o));
		while (it.hasNext()) {
			this.resultTriples.add(it.next());
		}
	}

	protected RDFNode resource(String relativeURI) {
		return m.createResource("http://test/" + relativeURI);
	}
	
	protected void dump() {
		int count = 0;
		for (Triple t: resultTriples) {
			count++;
			System.out.println("Result Triple " + count + ": " + 
					PrettyPrinter.toString(t, this.graph.getPrefixMapping()));
		}
		System.out.println(count + " triples.");
		System.out.println();
	}

	protected void assertStatementCount(int count) {
		assertEquals(count, this.resultTriples.size());
	}
	
	protected void assertStatement(RDFNode s, RDFNode p, RDFNode o) {
		assertTrue(this.resultTriples.contains(new Triple(toNode(s), toNode(p), toNode(o))));
	}
	
	protected void assertNoStatement(RDFNode s, RDFNode p, RDFNode o) {
		assertFalse(this.resultTriples.contains(new Triple(toNode(s), toNode(p), toNode(o))));
	}
	
	private Node toNode(RDFNode n) {
		if (n == null) {
			return Node.ANY;
		}
		return n.asNode();
	}
}
