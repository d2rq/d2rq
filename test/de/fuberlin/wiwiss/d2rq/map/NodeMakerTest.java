package de.fuberlin.wiwiss.d2rq.map;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

import junit.framework.TestCase;

public class NodeMakerTest extends TestCase {

	public void testFixedNodeMakerToString() {
		assertEquals("Fixed(\"foo\")", 
				new FixedNodeMaker(Node.createLiteral("foo")).toString());
		assertEquals("Fixed(\"foo\"@en)", 
				new FixedNodeMaker(Node.createLiteral("foo", "en", null)).toString());
		assertEquals("Fixed(\"1\"^^<" + XSDDatatype.XSDint.getURI() + ">)",
				new FixedNodeMaker(Node.createLiteral("1", null, XSDDatatype.XSDint)).toString());
		assertEquals("Fixed(_:foo)", 
				new FixedNodeMaker(Node.createAnon(new AnonId("foo"))).toString());
		assertEquals("Fixed(<http://example.org/>)", 
				new FixedNodeMaker(Node.createURI("http://example.org/")).toString());
	}
}
