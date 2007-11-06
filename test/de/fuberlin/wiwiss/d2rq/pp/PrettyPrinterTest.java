package de.fuberlin.wiwiss.d2rq.pp;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class PrettyPrinterTest extends TestCase {
	
	public void testNodePrettyPrinting() {
		assertEquals("\"foo\"", 
				PrettyPrinter.toString(Node.createLiteral("foo")));
		assertEquals("\"foo\"@en", 
				PrettyPrinter.toString(Node.createLiteral("foo", "en", null)));
		assertEquals("\"1\"^^<" + XSDDatatype.XSDint.getURI() + ">",
				PrettyPrinter.toString(Node.createLiteral("1", null, XSDDatatype.XSDint)));
		assertEquals("\"1\"^^xsd:int",
				PrettyPrinter.toString(Node.createLiteral("1", null, XSDDatatype.XSDint), PrefixMapping.Standard));
		assertEquals("_:foo", 
				PrettyPrinter.toString(Node.createAnon(new AnonId("foo"))));
		assertEquals("<http://example.org/>", 
				PrettyPrinter.toString(Node.createURI("http://example.org/")));
		assertEquals("<" + RDF.type.getURI() + ">", 
				PrettyPrinter.toString(RDF.type.asNode(), new PrefixMappingImpl()));
		assertEquals("rdf:type", 
				PrettyPrinter.toString(RDF.type.asNode(), PrefixMapping.Standard));
		assertEquals("?x", 
				PrettyPrinter.toString(Node.createVariable("x")));
		assertEquals("?ANY",
				PrettyPrinter.toString(Node.ANY));
	}
	
	public void testTriplePrettyPrinting() {
		assertEquals("<http://example.org/a> <" + RDFS.label.getURI() + "> \"Example\" .",
				PrettyPrinter.toString(new Triple(
						Node.createURI("http://example.org/a"),
						RDFS.label.asNode(),
						Node.createLiteral("Example", null, null))));
	}

	public void testTriplePrettyPrintingWithNodeANY() {
		assertEquals("?ANY ?ANY ?ANY .", PrettyPrinter.toString(Triple.ANY));
	}
	
	public void testTriplePrettyPrintingWithPrefixMapping() {
		PrefixMappingImpl prefixes = new PrefixMappingImpl();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "http://example.org/");
		assertEquals("ex:a rdfs:label \"Example\" .",
				PrettyPrinter.toString(new Triple(
						Node.createURI("http://example.org/a"),
						RDFS.label.asNode(),
						Node.createLiteral("Example", null, null)), prefixes));
	}
	
	public void testResourcePrettyPrinting() {
		Model m = ModelFactory.createDefaultModel();
		assertEquals("\"foo\"", PrettyPrinter.toString(m.createLiteral("foo")));
		assertEquals("<http://test/>", PrettyPrinter.toString(m.createResource("http://test/")));
	}
	
	public void testUsePrefixMappingWhenPrintingURIResources() {
		Model m = ModelFactory.createDefaultModel();
		m.setNsPrefix("ex", "http://example.org/");
		assertEquals("ex:foo", PrettyPrinter.toString(m.createResource("http://example.org/foo")));
	}
	
	public void testD2RQTermsHaveD2RQPrefix() {
		assertEquals("d2rq:ClassMap", PrettyPrinter.toString(D2RQ.ClassMap));
	}
	
	public void testSomeRDFDatatypeToString() {
		RDFDatatype someDatatype = TypeMapper.getInstance().getSafeTypeByName("http://example.org/mytype");
		assertEquals("<http://example.org/mytype>", PrettyPrinter.toString(someDatatype));
	}
	
	public void testXSDTypeToString() {
		assertEquals("xsd:string", PrettyPrinter.toString(XSDDatatype.XSDstring));
	}
}
