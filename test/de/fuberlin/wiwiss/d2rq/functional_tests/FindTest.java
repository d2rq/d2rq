/*
 * $Id: FindTest.java,v 1.1 2004/08/10 23:14:14 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * Functional tests for the find(spo) operation of {@link de.fuberlin.wiwiss.d2rq.GraphD2RQ}.
 * Runs different find queries against the ISWC database, using the example map
 * provided with the D2RQ manual. To run the test, you must have either the MySQL or the
 * MS Access version accessible. Maybe you must adapt the connection information at the
 * beginning of the map file to fit your database server.
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
public class FindTest extends TestCase {
	private final static String D2RQMap = "file:doc/manual/ISWC-d2rq.n3";
	private final static String NS = "http://annotation.semanticweb.org/iswc/iswc.daml#";
	private final static RDFDatatype xsdString =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
	private final static RDFDatatype xsdYear =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");

	private GraphD2RQ graph;
	private Set resultTriples; 
    
	/**
	 * Constructor for FindTest.
	 * @param arg0
	 */
	public FindTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		this.graph = new GraphD2RQ(D2RQMap);
//		this.graph.enableDebug();
	}

	protected void tearDown() throws Exception {
		this.graph.close();
	}

	public void testListTypeStatements() {
		find(Node.ANY, RDF.Nodes.type, Node.ANY);
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper1"),
				RDF.Nodes.type,
				Node.createURI(NS + "InProceedings"));
		// Paper6 is filtered by d2rq:condition
		assertNoTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper6"),
				RDF.Nodes.type,
				Node.createURI(NS + "InProceedings"));
		assertTriple(
				Node.createURI("http://conferences.org/comp/confno23541"),
				RDF.Nodes.type,
				Node.createURI(NS + "Conference"));
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@15")),
				RDF.Nodes.type,
				Node.createURI(NS + "Topic"));
		assertTripleCount(29);
	}

	public void testListTopicInstances() {
		find(Node.ANY, RDF.Nodes.type, Node.createURI(NS + "Topic"));
//		dump();
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@1")),
				RDF.Nodes.type,
				Node.createURI(NS + "Topic"));
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@15")),
				RDF.Nodes.type,
				Node.createURI(NS + "Topic"));
		assertTripleCount(15);
	}

	public void testListTopicNames() {
		find(Node.ANY, Node.createURI(NS + "name"), Node.ANY);
//		dump();
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@1")),
				Node.createURI(NS + "name"),
				Node.createLiteral("Knowledge Representation Languages", null, xsdString));
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@15")),
				Node.createURI(NS + "name"),
				Node.createLiteral("Knowledge Management", null, xsdString));
		assertTripleCount(15);
	}

	public void testListAuthors() {
		find(Node.ANY, Node.createURI(NS + "author_of"), Node.ANY);
//		dump();
		assertTriple(
				Node.createURI("http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Yolanda Gil"),
				Node.createURI(NS + "author_of"),
				Node.createURI("http://www.conference.org/conf02004/paper#Paper1"));
		assertTriple(
				Node.createURI("http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Varun Ratnakar"),
				Node.createURI(NS + "author_of"),
				Node.createURI("http://www.conference.org/conf02004/paper#Paper1"));
		assertTripleCount(7);
	}
	
	public void testLiteralDatatype() {
		find(Node.ANY, Node.createURI(NS + "year"), Node.createLiteral("2003", null, xsdYear));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper4"),
				Node.createURI(NS + "year"),
				Node.createLiteral("2003", null, xsdYear));
		assertTripleCount(1);

		find(Node.ANY, Node.createURI(NS + "name"), Node.createLiteral("E-Business", null, xsdString));
//		dump();
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@13")),
				Node.createURI(NS + "name"),
				Node.createLiteral("E-Business", null, xsdString));
		assertTripleCount(1);

		find(Node.ANY, Node.createURI(NS + "name"), Node.createLiteral("E-Business", null, null));
//		dump();
		assertTripleCount(0);

		find(Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "year"), Node.ANY);
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "year"),
				Node.createLiteral("2002", null, xsdYear));
		assertTripleCount(1);

		find(Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "year"), Node.createLiteral("2002", null, xsdYear));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "year"),
				Node.createLiteral("2002", null, xsdYear));
		assertTripleCount(1);
	}

	public void testLiteralLanguage() {
		find(Node.ANY, Node.createURI(NS + "title"),
				Node.createLiteral("Titel of the Paper: Trusting Information Sources One Citizen at a Time", "en", null));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper1"),
				Node.createURI(NS + "title"),
				Node.createLiteral("Titel of the Paper: Trusting Information Sources One Citizen at a Time", "en", null));
		assertTripleCount(1);
	}

	public void testFindSubjectWhereObjectURIColumn() {
		find(Node.ANY, Node.createURI(NS + "author"),
				Node.createURI("http://www.i-u.de/schools/eberhart/iswc2002/#Andreas Eberhart"));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "author"),
				Node.createURI("http://www.i-u.de/schools/eberhart/iswc2002/#Andreas Eberhart"));
		assertTripleCount(1);
    }

	public void testFindSubjectWithConditionalObject() {
		// The paper is not published, therefore no result triples
		find(Node.ANY, Node.createURI(NS + "author"),
				Node.createURI("http://www.cs.vu.nl/~borys#Bomelayenko"));
//		dump();
		assertTripleCount(0);		
	}

	public void testFindSubjectWhereObjectURIPattern() {
		find(Node.ANY, Node.createURI(NS + "eMail"),
				Node.createURI("mailto:andy.seaborne@hpl.hp.com"));
//		dump();
		assertTriple(
				Node.createURI("http://www-uk.hpl.hp.com/people#andy_seaborne"),
				Node.createURI(NS + "eMail"),
				Node.createURI("mailto:andy.seaborne@hpl.hp.com"));
		assertTripleCount(1);
    }

	public void testMatchAnonymousNode() {
		find(Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3")),
				Node.createURI(NS + "name"), Node.ANY);
//		dump();
		assertTriple(
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3")),
				Node.createURI(NS + "name"),
				Node.createLiteral("Artificial Intelligence", null, xsdString));
		assertTripleCount(1);

		find(Node.ANY, Node.createURI(NS + "primaryTopic"),
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3")));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper5"),
				Node.createURI(NS + "primaryTopic"),
				Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3")));
		assertTripleCount(1);
	}

	public void testDump() {
		find(Node.ANY, Node.ANY, Node.ANY);
//		dump();
		assertTripleCount(139);
	}

	public void testFetchAnonAndReverse() {
		Node topic3 = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@3"));
		find(topic3, Node.ANY, Node.ANY);
//		dump();
		assertTriple(
				topic3,
				Node.createURI(NS + "name"),
				Node.createLiteral("Artificial Intelligence", null, xsdString));
		assertTriple(
				topic3,
				RDF.Nodes.type,
				Node.createURI(NS + "Topic"));
		assertTripleCount(2);

		find(Node.ANY, Node.ANY, topic3);
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper5"),
				Node.createURI(NS + "primaryTopic"),
				topic3);
		assertTriple(
				Node.createURI("http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Yolanda Gil"),
				Node.createURI(NS + "research_topic"),
				topic3);
		assertTriple(
				Node.createURI("http://trellis.semanticweb.org/expect/web/semanticweb/iswc02_trellis.pdf#Jim Blythe"),
				Node.createURI(NS + "research_topic"),
				topic3);
		assertTripleCount(3);
	}

	public void testFindPredicate() {
		find(Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.ANY, Node.createLiteral("2002", null, xsdYear));
//		dump();
		assertTriple(
				Node.createURI("http://www.conference.org/conf02004/paper#Paper2"),
				Node.createURI(NS + "year"),
				Node.createLiteral("2002", null, xsdYear));
		assertTripleCount(1);
    }

	public void testReverseFetchWithDatatype() {
		find(Node.ANY, Node.ANY, Node.createLiteral("2002", null, xsdYear));
//		dump();
		assertTripleCount(3);
	}

	public void testReverseFetchWithURI() {
		find(Node.ANY, Node.ANY, Node.createURI("http://www.conference.org/conf02004/paper#Paper1"));
//		dump();
		assertTripleCount(2);
	}

	private void find(Node s, Node p, Node o) {
		this.resultTriples = new HashSet();
		ExtendedIterator it = this.graph.find(s, p, o);
		while (it.hasNext()) {
			this.resultTriples.add(it.next());
		}
	}
	
	void dump() {
		int count = 1;
		Iterator it = this.resultTriples.iterator();
		while (it.hasNext()) {
			System.out.println("Result Triple " + count + ": " + it.next());
			count++;
		}
		System.out.println();
	}

	private void assertTripleCount(int count) {
		assertEquals(count, this.resultTriples.size());
	}
	
	private void assertTriple(Node s, Node p, Node o) {
		assertTrue(this.resultTriples.contains(new Triple(s, p, o)));
	}
	
	private void assertNoTriple(Node s, Node p, Node o) {
		assertFalse(this.resultTriples.contains(new Triple(s, p, o)));
	}
}
