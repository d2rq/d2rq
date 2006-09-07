package de.fuberlin.wiwiss.d2rq.functional_tests;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.helpers.QueryLanguageTestFramework;
import de.fuberlin.wiwiss.d2rq.vocab.ISWC;
import de.fuberlin.wiwiss.d2rq.vocab.SKOS;


/**
 * Functional tests that exercise a ModelD2RQ by running RDQL queries against it. 
 * For notes on running the tests, see {@link AllTests}.
 * 
 * Each test method runs one RDQL query and automatically compares the actual
 * results to the expected results. For some tests, only the number of returned triples
 * is checked. For others, the returned values are compared against expected values.
 * 
 * If a test fails, the dump() method can be handy. It shows the actual results returned
 * by a query on System.out.
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RDQLTest.java,v 1.14 2006/09/07 21:33:20 cyganiak Exp $
 */
public class RDQLTest extends QueryLanguageTestFramework {

	protected String mapURL() {
		return D2RQTestSuite.ISWC_MAP;
	}

	public void testRDQLFetch() {
		rdql("SELECT ?x, ?y WHERE (<http://test/papers/1>, ?x, ?y)");
//		dump();
		
		expectVariable("x", ISWC.conference);
		expectVariable("y", this.model.createResource("http://test/conferences/23541"));
		assertSolution();

		expectVariable("x", SKOS.primarySubject);
		expectVariable("y", this.model.createResource("http://test/topics/5"));
		assertSolution();

		expectVariable("x", SKOS.subject);
		expectVariable("y", this.model.createResource("http://test/topics/15"));
		assertSolution();

		expectVariable("x", DC.date);
		expectVariable("y", this.model.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertSolution();

		expectVariable("x", DC.title);
		expectVariable("y", this.model.createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
		assertSolution();

		expectVariable("x", RDF.type);
		expectVariable("y", ISWC.InProceedings);
		assertSolution();

		assertResultCount(12);
	}
	
	public void testRDQLGetAuthorsAndEmails() {
		rdql("SELECT ?x, ?y WHERE (?x, dc:creator, ?z), (?z, foaf:mbox, ?y)");
//		dump();

		expectVariable("x", this.model.createResource("http://test/papers/1"));
		expectVariable("y", this.model.createResource("mailto:gil@isi.edu"));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/1"));
		expectVariable("y", this.model.createResource("mailto:varunr@isi.edu"));
		assertSolution();

		assertResultCount(7);
	}

	public void testRDQLGetAuthorsAndEmailsWithCondition() {
	    // GraphD2RQ.setUsingD2RQQueryHandler(false);
	    // mysql must be prepared for ANSI expressions with:
	    // SET GLOBAL sql_mode = 'REAL_AS_FLOAT,PIPES_AS_CONCAT,ANSI_QUOTES,IGNORE_SPACE';
		rdql("SELECT ?x, ?y WHERE (?x, dc:creator, ?z), (?z, foaf:mbox, ?y) AND ! (?x eq ?z)");
//		dump();

		expectVariable("x", this.model.createResource("http://test/papers/1"));
		expectVariable("y", this.model.createResource("mailto:gil@isi.edu"));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/1"));
		expectVariable("y", this.model.createResource("mailto:varunr@isi.edu"));
		assertSolution();

		assertResultCount(7);
	}

	
	public void testRDQLGetTopics() {
		rdql("SELECT ?x, ?y WHERE (?x, skos:primarySubject, ?z), (?z, skos:prefLabel, ?y)");
//		dump();

		expectVariable("x", this.model.createResource("http://test/papers/1"));
		expectVariable("y", this.model.createTypedLiteral("Semantic Web", XSDDatatype.XSDstring));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/4"));
		expectVariable("y", this.model.createTypedLiteral("Semantic Web Infrastructure", XSDDatatype.XSDstring));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/5"));
		expectVariable("y", this.model.createTypedLiteral("Artificial Intelligence", XSDDatatype.XSDstring));
		assertSolution();

		assertResultCount(3);
	}

	public void testRDQLGetAuthorsOfPaperByTitle() {
		rdql("SELECT ?x, ?y WHERE (?x, dc:creator, ?y), (?x, dc:title, 'Three Implementations of SquishQL, a Simple RDF Query Language'@en)");
//		dump();

		expectVariable("x", this.model.createResource("http://test/papers/4"));
		expectVariable("y", this.model.createResource("http://test/persons/6"));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/4"));
		expectVariable("y", this.model.createResource("http://test/persons/9"));
		assertSolution();

		assertResultCount(2);
	}

	public void testRDQLGetAuthorsNameAndEmail() {
		rdql("SELECT ?x, ?y, ?a WHERE (?x, dc:creator, ?y), (?x, dc:title, 'Three Implementations of SquishQL, a Simple RDF Query Language'@en), (?y, foaf:mbox, ?a)");
//		dump();

		expectVariable("x", this.model.createResource("http://test/papers/4"));
		expectVariable("y", this.model.createResource("http://test/persons/6"));
		expectVariable("a", this.model.createResource("mailto:andy.seaborne@hpl.hp.com"));
		assertSolution();

		expectVariable("x", this.model.createResource("http://test/papers/4"));
		expectVariable("y", this.model.createResource("http://test/persons/9"));
		expectVariable("a", this.model.createResource("mailto:areggiori@webweaving.org"));
		assertSolution();

		assertResultCount(2);
	}

	public void testGetTitleAndYearOfAllPapers() {
		rdql("SELECT ?title, ?year WHERE (?paper, dc:title, ?title), (?paper, dc:date, ?year)");
//		dump();

		expectVariable("title", this.model.createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
		expectVariable("year", this.model.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertSolution();
		
		assertResultCount(4);
	}
}
