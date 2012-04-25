package de.fuberlin.wiwiss.d2rq.functional_tests;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;

/**
 * Functional tests that exercise a ModelD2RQ by calling Model API functions. For
 * notes on running the tests, see {@link AllTests}. 
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ModelAPITest extends TestCase {
	private ModelD2RQ model;

	protected void setUp() throws Exception {
		this.model = new ModelD2RQ(D2RQTestSuite.ISWC_MAP, "TURTLE", "http://test/");
//		this.model.enableDebug();
	}

	protected void tearDown() throws Exception {
		this.model.close();
	}

	public void testListStatements() {
		StmtIterator iter = this.model.listStatements();
		int count = 0;
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			stmt.toString();
//			dumpStatement(stmt);
			count++;
		}
		assertEquals(322, count);
	}

	public void testHasProperty() {
		assertTrue(this.model.getResource("http://test/papers/1").hasProperty(DC.creator));
	}

	void dumpStatement(Statement stmt) {
		Resource  subject   = stmt.getSubject();
		Property  predicate = stmt.getPredicate();
		RDFNode   object    = stmt.getObject();
		System.out.print(subject + " " + predicate + " ");
		if (object instanceof Resource) {
			System.out.print(object);
		} else { // object is a literal
			System.out.print(" \"" + object + "\"");
		}
		System.out.println(" .");
	}
}
