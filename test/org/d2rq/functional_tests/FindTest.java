package org.d2rq.functional_tests;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.D2RQTestSuite;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.jena.ModelD2RQ;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.vocab.ISWC;
import org.d2rq.vocab.SKOS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.VCARD;


/**
 * Functional tests for the find(spo) operation of {@link org.d2rq.jena.GraphD2RQ}.
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
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class FindTest {
    private GraphD2RQ graph;
	private Set<Triple> resultTriples; 

	@Before
	public void setUp() throws Exception {
		graph = new ModelD2RQ(D2RQTestSuite.ISWC_MAP, "http://test/").getGraph();
	}

	@After
	public void tearDown() throws Exception {
		graph.close();
	}

	@Test
	public void testListTypeStatements() {
		find(null, RDF.type, null);
//		dump();
		assertStatement(resource("papers/1"), RDF.type, ISWC.InProceedings);
		// Paper6 is filtered by d2rq:condition
		assertNoStatement(resource("papers/6"), RDF.type, ISWC.InProceedings);
		assertStatement(resource("conferences/23541"), RDF.type, ISWC.Conference);
		assertStatement(resource("topics/15"), RDF.type, SKOS.Concept);
		assertStatementCount(95);
	}

	@Test
	public void testListTopicInstances() {
		find(null, RDF.type, SKOS.Concept);
//		dump();
		assertStatement(resource("topics/1"), RDF.type, SKOS.Concept);
		assertStatement(resource("topics/15"), RDF.type, SKOS.Concept);
		assertStatementCount(15);
	}

	@Test
	public void testListTopicNames() {
		find(null, SKOS.prefLabel, null);
//		dump();
		assertStatement(resource("topics/1"), SKOS.prefLabel, createTypedLiteral(
				"Knowledge Representation Languages"));
		assertStatement(resource("topics/15"), SKOS.prefLabel, createTypedLiteral(
				"Knowledge Management"));
		assertStatementCount(15);
	}

	@Test
	public void testListAuthors() {
		find(null, DC.creator, null);
//		dump();
		assertStatement(resource("papers/1"), DC.creator, resource("persons/1"));
		assertStatement(resource("papers/1"), DC.creator, resource("persons/2"));
		assertStatementCount(8);
	}
	
	@Test
	public void testDatatypeFindByYear() {
		find(null, DC.date, createTypedLiteral("2003", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/4"), DC.date, createTypedLiteral("2003", XSDDatatype.XSDgYear));
		assertStatementCount(1);
	}
	
	@Test
	public void testDatatypeFindByString() {
		find(null, SKOS.prefLabel, createTypedLiteral("E-Business", XSDDatatype.XSDstring));
//		dump();
		assertStatement(resource("topics/13"), SKOS.prefLabel, createTypedLiteral("E-Business", XSDDatatype.XSDstring));
		assertStatementCount(1);
	}
	
	@Test
	public void testXSDStringDoesntMatchPlainLiteral() {
		find(null, SKOS.prefLabel, createPlainLiteral("E-Business"));
//		dump();
		assertStatementCount(0);
	}
	
	@Test
	public void testDatatypeFindYear() {
		find(resource("papers/2"), DC.date, null);
//		dump();
		assertStatement(resource("papers/2"), DC.date, createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
	}
	
	@Test
	public void testDatatypeYearContains() {
		find(resource("papers/2"), DC.date, createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/2"), DC.date, createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
	}

	@Test
	public void testLiteralLanguage() {
		find(null, DC.title, createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
//		dump();
		assertStatement(resource("papers/1"), DC.title, createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
		assertStatementCount(1);
	}

	@Test
	public void testFindSubjectWhereObjectURIColumn() {
		find(null, DC.creator, resource("persons/4"));
//		dump();
		assertStatement(resource("papers/2"), DC.creator, resource("persons/4"));
		assertStatementCount(1);
    }

	@Test
	public void testFindSubjectWithConditionalObject() {
		// The paper is not published, therefore no result triples
		find(null, DC.creator, resource("persons/5"));
//		dump();
		assertStatementCount(0);
	}

	@Test
	public void testFindSubjectWhereObjectURIPattern() {
		find(null, FOAF.mbox, createResource("mailto:andy.seaborne@hpl.hp.com"));
//		dump();
		assertStatement(resource("persons/6"), FOAF.mbox, createResource("mailto:andy.seaborne@hpl.hp.com"));
		assertStatementCount(1);
    }

	@Test
	public void testFindAnonymousNode() {
		find(null, VCARD.Pcode, createPlainLiteral("BS34 8QZ"));
//		dump();
		assertStatement(
				createAnonResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, createPlainLiteral("BS34 8QZ"));
		assertStatementCount(1);
	}

	@Test
	public void testMatchAnonymousSubject() {
		find(
				createAnonResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, null);
//		dump();
		assertStatement(
				createAnonResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, createPlainLiteral("BS34 8QZ"));
		assertStatementCount(1);
	}
	
	@Test
	public void testMatchAnonymousObject() {
		find(
				null, VCARD.ADR,
				createAnonResource(new AnonId("map:PostalAddresses@@7")));
//		dump();
		assertStatement(
				resource("organizations/7"), VCARD.ADR, 
				createAnonResource(new AnonId("map:PostalAddresses@@7")));
		assertStatementCount(1);
	}

	@Test
	public void testDump() {
		find(null, null, null);
//		dump();
		assertStatementCount(322);
	}

	@Test
	public void testFindPredicate() {
		find(resource("papers/2"), null, createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/2"), DC.date, createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
    }

	@Test
	public void testReverseFetchWithDatatype() {
		find(null, null, createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatementCount(3);
	}

	@Test
	public void testReverseFetchWithURI() {
		find(null, null, resource("topics/11"));
//		dump();
		assertStatementCount(2);
	}
	
	@Test
	public void testFindAliasedPropertyBridge() {
		find(null, SKOS.broader, null);
//		dump();
		assertStatement(resource("topics/1"), SKOS.broader, resource("topics/3"));
		assertStatementCount(10);
	}
	
	@Test
	public void testDefinitions() {
		find(ISWC.Conference, null, null);
		assertStatement(ISWC.Conference, RDF.type, RDFS.Class);
		assertStatement(ISWC.Conference, RDFS.label, createPlainLiteral("conference"));
		assertStatement(ISWC.Conference, RDFS.comment, createPlainLiteral("A conference"));
		assertStatement(ISWC.Conference, RDFS.subClassOf, ISWC.Event);
		find(RDFS.label, null, null);
		assertStatement(RDFS.label, RDF.type, RDF.Property);
		assertStatement(RDFS.label, RDFS.label, createPlainLiteral("label"));
		assertStatement(RDFS.label, RDFS.comment, createPlainLiteral("A human-readable name for the subject."));
		assertStatement(RDFS.label, RDFS.domain, RDFS.Resource);
	}
	
	private void find(RDFNode s, RDFNode p, RDFNode o) {
		this.resultTriples = new HashSet<Triple>();
		ExtendedIterator<Triple> it = this.graph.find(toNode(s), toNode(p), toNode(o));
		while (it.hasNext()) {
			this.resultTriples.add(it.next());
		}
	}

	private RDFNode resource(String relativeURI) {
		return createResource("http://test/" + relativeURI);
	}
	
	public void dump() {
		int count = 0;
		for (Triple t: resultTriples) {
			count++;
			System.out.println("Result Triple " + count + ": " + 
					PrettyPrinter.toString(t, this.graph.getPrefixMapping()));
		}
		System.out.println(count + " triples.");
		System.out.println();
	}

	private void assertStatementCount(int count) {
		assertEquals(count, this.resultTriples.size());
	}
	
	private void assertStatement(RDFNode s, RDFNode p, RDFNode o) {
		assertTrue(this.resultTriples.contains(new Triple(toNode(s), toNode(p), toNode(o))));
	}
	
	private void assertNoStatement(RDFNode s, RDFNode p, RDFNode o) {
		assertFalse(this.resultTriples.contains(new Triple(toNode(s), toNode(p), toNode(o))));
	}
	
	private Node toNode(RDFNode n) {
		return n == null ? Node.ANY : n.asNode();
	}
	
	public static Resource createAnonResource(AnonId id) {
		return m.createResource(id);
	}
	public static Literal createLiteral(String lexicalForm, String language) {
		return m.createLiteral(lexicalForm, language);
	}
	private final static Model m = ModelFactory.createDefaultModel();
}
