package org.d2rq.functional_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.d2rq.D2RQTestSuite;
import org.d2rq.jena.ModelD2RQ;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;


/**
 * Functional tests that exercise a ModelD2RQ by calling Model API functions. For
 * notes on running the tests, see {@link AllTests}. 
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ModelAPITest {
	private ModelD2RQ model;

	@Before
	public void setUp() throws Exception {
		model = new ModelD2RQ(D2RQTestSuite.ISWC_MAP, "http://test/");
	}

	@After
	public void tearDown() throws Exception {
		model.close();
	}

	@Test
	public void testListStatements() {
		StmtIterator iter = model.listStatements();
		int count = 0;
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			stmt.toString();
			count++;
		}
		assertEquals(322, count);
	}

	@Test
	public void testHasProperty() {
		assertTrue(model.getResource("http://test/papers/1").hasProperty(DC.creator));
	}
}
