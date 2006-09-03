package de.fuberlin.wiwiss.d2rq.functional_tests;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.TestFramework;

/**
 * Functional tests that exercise a ModelD2RQ by calling Model API functions. For
 * notes on running the tests, see {@link AllTests}. 
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ModelAPITest.java,v 1.5 2006/09/03 00:08:10 cyganiak Exp $
 */
public class ModelAPITest extends TestFramework {
	private ModelD2RQ model;

	protected void setUp() throws Exception {
		this.model = new ModelD2RQ(D2RQMap);
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
		assertEquals(139, count);
	}

	public void testHasProperty() {
		Resource paperRessource = this.model.getResource("http://www.conference.org/conf02004/paper#Paper1");
		Property author = this.model.createProperty(NS + "author");
		assertTrue(paperRessource.hasProperty(author));
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
