package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * Functional tests for the find(spo) operation of {@link de.fuberlin.wiwiss.d2rq.GraphD2RQ}.
 * For notes on running the tests, see {@link AllTests}.
 * 
 * Each test method runs one or more find queries and automatically compares the actual
 * results to the expected results. For some tests, only the number of returned triples
 * is checked. For others, the returned triples are compared against expected triples.
 * 
 * If a test fails, the dump() method can be handy. It shows the actual triples returned
 * by a query on System.out.
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class FindTestFramework extends TestFramework {

	protected GraphD2RQ graph;
	protected Set resultTriples; 

	/**
	 * @param arg0
	 */
	public FindTestFramework(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		this.graph = new GraphD2RQ(D2RQMap);
//		this.graph.enableDebug();
	}

	protected void tearDown() throws Exception {
		this.graph.close();
	}

	protected void find(Node s, Node p, Node o) {
		this.resultTriples = new HashSet();
		ExtendedIterator it = this.graph.find(s, p, o);
		while (it.hasNext()) {
			this.resultTriples.add(it.next());
		}
	}
	
	protected void dump() {
		int count = 1;
		Iterator it = this.resultTriples.iterator();
		while (it.hasNext()) {
			System.out.println("Result Triple " + count + ": " + it.next());
			count++;
		}
		System.out.println();
	}

	protected void assertTripleCount(int count) {
		assertEquals(count, this.resultTriples.size());
	}
	
	protected void assertTriple(Node s, Node p, Node o) {
		assertTrue(this.resultTriples.contains(new Triple(s, p, o)));
	}
	
	protected void assertNoTriple(Node s, Node p, Node o) {
		assertFalse(this.resultTriples.contains(new Triple(s, p, o)));
	}

}
