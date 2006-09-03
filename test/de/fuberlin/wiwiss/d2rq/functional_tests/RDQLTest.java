package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.AnonId;

import de.fuberlin.wiwiss.d2rq.helpers.RDQLTestFramework;


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
 * @version $Id: RDQLTest.java,v 1.12 2006/09/03 13:03:42 cyganiak Exp $
 */
public class RDQLTest extends RDQLTestFramework {

	public void testRDQLFetch() {
		rdql("SELECT ?x, ?y WHERE (<http://www.conference.org/conf02004/paper#Paper1>, ?x, ?y)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource(NS + "conference"));
		aResult.put("y", this.model.createResource("http://conferences.org/comp/confno23541"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource(NS + "primaryTopic"));
		aResult.put("y", this.model.createResource(new AnonId("http://www.example.org/dbserver01/db01#Topic@@5")));
		assertResult(aResult);

		aResult.put("x", this.model.createResource(NS + "secondaryTopic"));
		aResult.put("y", this.model.createResource(new AnonId("http://www.example.org/dbserver01/db01#Topic@@15")));
		assertResult(aResult);

		aResult.put("x", this.model.createResource(NS + "year"));
		aResult.put("y", this.model.createTypedLiteral("2002", xsdYear));
		assertResult(aResult);

		aResult.put("x", this.model.createResource(NS + "title"));
		aResult.put("y", this.model.createLiteral("Trusting Information Sources One Citizen at a Time (Full paper)", "en"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		aResult.put("y", this.model.createResource(NS + "InProceedings"));
		assertResult(aResult);

		assertResultCount(10);
	}
	
	public void testRDQLGetAuthorsAndEmails() {
		rdql("SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?z), " +
								"(?z, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?y)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper1"));
		aResult.put("y", this.model.createResource("mailto:gil@isi.edu"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper1"));
		aResult.put("y", this.model.createResource("mailto:varunr@isi.edu"));
		assertResult(aResult);

		assertResultCount(7);
	}

	public void testRDQLGetAuthorsAndEmailsWithCondition() {
	    // GraphD2RQ.setUsingD2RQQueryHandler(false);
	    // mysql must be prepared for ANSI expressions with:
	    // SET GLOBAL sql_mode = 'REAL_AS_FLOAT,PIPES_AS_CONCAT,ANSI_QUOTES,IGNORE_SPACE';
		rdql("SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?z), " +
								"(?z, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?y)" +
								" AND ! (?x eq ?z)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper1"));
		aResult.put("y", this.model.createResource("mailto:gil@isi.edu"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper1"));
		aResult.put("y", this.model.createResource("mailto:varunr@isi.edu"));
		assertResult(aResult);

		assertResultCount(7);
	}

	
	public void testRDQLGetTopics() {
		rdql("SELECT ?x, ?z, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#primaryTopic>, ?z), (?z, <http://annotation.semanticweb.org/iswc/iswc.daml#name>, ?y)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper1"));
		aResult.put("y", this.model.createTypedLiteral("Semantic Web", xsdString));
		aResult.put("z", this.model.createResource(new AnonId("http://www.example.org/dbserver01/db01#Topic@@5")));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper4"));
		aResult.put("y", this.model.createTypedLiteral("Semantic Web Infrastructure", xsdString));
		aResult.put("z", this.model.createResource(new AnonId("http://www.example.org/dbserver01/db01#Topic@@11")));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper5"));
		aResult.put("y", this.model.createTypedLiteral("Artificial Intelligence", xsdString));
		aResult.put("z", this.model.createResource(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3")));
		assertResult(aResult);

		assertResultCount(3);
	}

	public void testRDQLGetAuthorsOfPaperByTitle() {
		rdql("SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Three Implementations of SquishQL, a Simple RDF Query Language (Full paper)'@en)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper4"));
		aResult.put("y", this.model.createResource("http://www-uk.hpl.hp.com/people#andy_seaborne"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper4"));
		aResult.put("y", this.model.createResource("http://reggiori.webweaving.org#Alberto Reggiori"));
		assertResult(aResult);

		assertResultCount(2);
	}

	public void testRDQLGetAuthorsNameAndEmail() {
		rdql("SELECT ?x, ?y, ?a WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Three Implementations of SquishQL, a Simple RDF Query Language (Full paper)'@en), (?y, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?a)");
//		dump();
		Map aResult = new HashMap();

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper4"));
		aResult.put("y", this.model.createResource("http://www-uk.hpl.hp.com/people#andy_seaborne"));
		aResult.put("a", this.model.createResource("mailto:andy.seaborne@hpl.hp.com"));
		assertResult(aResult);

		aResult.put("x", this.model.createResource("http://www.conference.org/conf02004/paper#Paper4"));
		aResult.put("y", this.model.createResource("http://reggiori.webweaving.org#Alberto Reggiori"));
		aResult.put("a", this.model.createResource("mailto:areggiori@webweaving.org"));
		assertResult(aResult);

		assertResultCount(2);
	}

	public void testGetTitleAndYearOfAllPapers() {
		rdql("SELECT ?title, ?year WHERE " +
				"(?paper, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, ?title), "+
				"(?paper, <http://annotation.semanticweb.org/iswc/iswc.daml#year>, ?year)");
		Map aResult = new HashMap();

		aResult.put("title", this.model.createLiteral("Trusting Information Sources One Citizen at a Time (Full paper)", "en"));
		aResult.put("year", this.model.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertResult(aResult);
		
		assertResultCount(4);
	}
}
