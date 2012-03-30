package de.fuberlin.wiwiss.d2rq.functional_tests;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.VCARD;

import de.fuberlin.wiwiss.d2rq.helpers.FindTestFramework;
import de.fuberlin.wiwiss.d2rq.vocab.ISWC;
import de.fuberlin.wiwiss.d2rq.vocab.SKOS;

/**
 * Functional tests for the find(spo) operation of {@link de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ}.
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
public class FindTest extends FindTestFramework {
    
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

	public void testListTopicInstances() {
		find(null, RDF.type, SKOS.Concept);
//		dump();
		assertStatement(resource("topics/1"), RDF.type, SKOS.Concept);
		assertStatement(resource("topics/15"), RDF.type, SKOS.Concept);
		assertStatementCount(15);
	}

	public void testListTopicNames() {
		find(null, SKOS.prefLabel, null);
//		dump();
		assertStatement(resource("topics/1"), SKOS.prefLabel, m.createTypedLiteral(
				"Knowledge Representation Languages"));
		assertStatement(resource("topics/15"), SKOS.prefLabel, m.createTypedLiteral(
				"Knowledge Management"));
		assertStatementCount(15);
	}

	public void testListAuthors() {
		find(null, DC.creator, null);
//		dump();
		assertStatement(resource("papers/1"), DC.creator, resource("persons/1"));
		assertStatement(resource("papers/1"), DC.creator, resource("persons/2"));
		assertStatementCount(8);
	}
	
	public void testDatatypeFindByYear() {
		find(null, DC.date, m.createTypedLiteral("2003", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/4"), DC.date, m.createTypedLiteral("2003", XSDDatatype.XSDgYear));
		assertStatementCount(1);
	}
	
	public void testDatatypeFindByString() {
		find(null, SKOS.prefLabel, m.createTypedLiteral("E-Business", XSDDatatype.XSDstring));
//		dump();
		assertStatement(resource("topics/13"), SKOS.prefLabel, m.createTypedLiteral("E-Business", XSDDatatype.XSDstring));
		assertStatementCount(1);
	}
	
	public void testXSDStringDoesntMatchPlainLiteral() {
		find(null, SKOS.prefLabel, m.createLiteral("E-Business"));
//		dump();
		assertStatementCount(0);
	}
	
	public void testDatatypeFindYear() {
		find(resource("papers/2"), DC.date, null);
//		dump();
		assertStatement(resource("papers/2"), DC.date, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
	}
	
	public void testDatatypeYearContains() {
		find(resource("papers/2"), DC.date, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/2"), DC.date, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
		assertStatementCount(1);
	}

	public void testLiteralLanguage() {
		find(null, DC.title, m.createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
//		dump();
		assertStatement(resource("papers/1"), DC.title, m.createLiteral("Trusting Information Sources One Citizen at a Time", "en"));
		assertStatementCount(1);
	}

	public void testFindSubjectWhereObjectURIColumn() {
		find(null, DC.creator, resource("persons/4"));
//		dump();
		assertStatement(resource("papers/2"), DC.creator, resource("persons/4"));
		assertStatementCount(1);
    }

	public void testFindSubjectWithConditionalObject() {
		// The paper is not published, therefore no result triples
		find(null, DC.creator, resource("persons/5"));
//		dump();
		assertStatementCount(0);
	}

	public void testFindSubjectWhereObjectURIPattern() {
		find(null, FOAF.mbox, m.createResource("mailto:andy.seaborne@hpl.hp.com"));
//		dump();
		assertStatement(resource("persons/6"), FOAF.mbox, m.createResource("mailto:andy.seaborne@hpl.hp.com"));
		assertStatementCount(1);
    }

	public void testFindAnonymousNode() {
		find(null, VCARD.Pcode, m.createLiteral("BS34 8QZ"));
//		dump();
		assertStatement(
				m.createResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, m.createLiteral("BS34 8QZ"));
		assertStatementCount(1);
	}

	public void testMatchAnonymousSubject() {
		find(
				m.createResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, null);
//		dump();
		assertStatement(
				m.createResource(new AnonId("map:PostalAddresses@@7")),
				VCARD.Pcode, m.createLiteral("BS34 8QZ"));
		assertStatementCount(1);
	}
	
	public void testMatchAnonymousObject() {
		find(
				null, VCARD.ADR,
				m.createResource(new AnonId("map:PostalAddresses@@7")));
//		dump();
		assertStatement(
				resource("organizations/7"), VCARD.ADR, 
				m.createResource(new AnonId("map:PostalAddresses@@7")));
		assertStatementCount(1);
	}

	public void testDump() {
		find(null, null, null);
//		dump();
		assertStatementCount(322);
	}

	public void testFindPredicate() {
		find(resource("papers/2"), null, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatement(resource("papers/2"), DC.date, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
		assertStatementCount(1);
    }

	public void testReverseFetchWithDatatype() {
		find(null, null, m.createTypedLiteral("2002", XSDDatatype.XSDgYear));
//		dump();
		assertStatementCount(3);
	}

	public void testReverseFetchWithURI() {
		find(null, null, resource("topics/11"));
//		dump();
		assertStatementCount(2);
	}
	
	public void testFindAliasedPropertyBridge() {
		find(null, SKOS.broader, null);
//		dump();
		assertStatement(resource("topics/1"), SKOS.broader, resource("topics/3"));
		assertStatementCount(10);
	}
	
	public void testDefinitions() {
		find(ISWC.Conference, null, null);
		assertStatement(ISWC.Conference, RDF.type, RDFS.Class);
		assertStatement(ISWC.Conference, RDFS.label, m.createLiteral("conference"));
		assertStatement(ISWC.Conference, RDFS.comment, m.createLiteral("A conference"));
		assertStatement(ISWC.Conference, RDFS.subClassOf, ISWC.Event);
		find(RDFS.label, null, null);
		assertStatement(RDFS.label, RDF.type, RDF.Property);
		assertStatement(RDFS.label, RDFS.label, m.createLiteral("label"));
		assertStatement(RDFS.label, RDFS.comment, m.createLiteral("A human-readable name for the subject."));
		assertStatement(RDFS.label, RDFS.domain, RDFS.Resource);
	}	
}
