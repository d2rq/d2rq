/*
 * $Id: RDQLTest.java,v 1.3 2005/03/02 13:03:48 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBinding.ResultBindingIterator;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

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
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class RDQLTest extends TestCase {
	private final static String D2RQMap = "file:doc/manual/ISWC-d2rq.n3";
	private final static String NS = "http://annotation.semanticweb.org/iswc/iswc.daml#";
	private final static RDFDatatype xsdString =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
	private final static RDFDatatype xsdYear =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");

private ModelD2RQ model;
	private Set results;

	public RDQLTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		this.model = new ModelD2RQ(D2RQMap);
//		this.model.enableDebug();
	}

	protected void tearDown() throws Exception {
		this.model.close();
		this.results = null;
	}

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
		aResult.put("y", this.model.createLiteral("Titel of the Paper: Trusting Information Sources One Citizen at a Time", "en"));
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
		rdql("SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Titel of the Paper: Three Implementations of SquishQL, a Simple RDF Query Language'@en)");
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
		rdql("SELECT ?x, ?y, ?a WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Titel of the Paper: Three Implementations of SquishQL, a Simple RDF Query Language'@en), (?y, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?a)");
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

	private void rdql(String rdql) {
		this.results = new HashSet();
		Query query = new Query(rdql);
		query.setSource(this.model);
		QueryResults qr = new QueryEngine(query).exec();
		while (qr.hasNext()) {
			ResultBinding binding = (ResultBinding) qr.next();
			this.results.add(binding);
		}
	}
	
	private void assertResultCount(int count) {
		assertEquals(count, this.results.size());
	}

	private void assertResult(Map map) {
		Iterator it = this.results.iterator();
		while (it.hasNext()) {
			ResultBinding binding = (ResultBinding) it.next();
			if (containsAll(binding, map)) {
				return;
			}
		}
		fail();
	}

	private boolean containsAll(ResultBinding actual, Map expected) {
		Iterator it = expected.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String actualResource = actual.get((String) entry.getKey()).toString();
			String expectedResource = entry.getValue().toString();
			if (!actualResource.equals(expectedResource)) {
				return false;
			}
		}
		return true;
	}
	
	protected void dump() {
		Iterator it = this.results.iterator();
		int count = 1;
		while (it.hasNext()) {
			System.out.println("Result binding " + count + ":");
			ResultBinding binding = (ResultBinding) it.next();
			ResultBindingIterator it2 = binding.iterator();
			while (it2.hasNext()) {
				it2.next();
				System.out.println("    " + it2.varName() + " => " + it2.value());
			}
			count++;
		}
	}
}
