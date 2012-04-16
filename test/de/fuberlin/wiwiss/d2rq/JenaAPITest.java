package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;

import junit.framework.TestCase;

public class JenaAPITest extends TestCase {

	public void testCopyPrefixesFromMapModelToD2RQModel() {
		ModelD2RQ m = new ModelD2RQ(D2RQTestSuite.DIRECTORY_URL + "prefixes.ttl");
		assertEquals("http://example.org/", m.getNsPrefixURI("ex"));
	}
	
	public void testCopyPrefixesFromMapModelToD2RQGraph() {
		GraphD2RQ g = new ModelD2RQ(
				FileManager.get().loadModel(D2RQTestSuite.DIRECTORY_URL + "prefixes.ttl"), null).getGraph();
		assertEquals("http://example.org/", g.getPrefixMapping().getNsPrefixURI("ex"));
	}
	
	public void testDontCopyD2RQPrefixFromMapModel() {
		ModelD2RQ m = new ModelD2RQ(D2RQTestSuite.DIRECTORY_URL + "prefixes.ttl");
		assertNull(m.getNsPrefixURI("d2rq"));
	}
}
