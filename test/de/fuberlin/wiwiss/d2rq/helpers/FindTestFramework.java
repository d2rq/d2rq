package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindTestFramework.java,v 1.1 2006/09/03 13:03:42 cyganiak Exp $
 */
public class FindTestFramework extends TestFramework {

	protected GraphD2RQ graph;
	protected Set resultTriples; 

	protected void setUp() throws Exception {
		this.graph = (GraphD2RQ) new ModelD2RQ(D2RQMap).getGraph();
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
