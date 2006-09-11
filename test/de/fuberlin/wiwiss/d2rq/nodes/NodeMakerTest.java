package de.fuberlin.wiwiss.d2rq.nodes;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Column;

public class NodeMakerTest extends TestCase {

	public void testFixedNodeMakerToString() {
		assertEquals("Fixed(\"foo\")", 
				new FixedNodeMaker(Node.createLiteral("foo"), true).toString());
		assertEquals("Fixed(\"foo\"@en)", 
				new FixedNodeMaker(Node.createLiteral("foo", "en", null), true).toString());
		assertEquals("Fixed(\"1\"^^<" + XSDDatatype.XSDint.getURI() + ">)",
				new FixedNodeMaker(Node.createLiteral("1", null, XSDDatatype.XSDint), true).toString());
		assertEquals("Fixed(_:foo)", 
				new FixedNodeMaker(Node.createAnon(new AnonId("foo")), true).toString());
		assertEquals("Fixed(<http://example.org/>)", 
				new FixedNodeMaker(Node.createURI("http://example.org/"), true).toString());
	}
	
	public void testBlankNodeMakerToString() {
		BlankNodeID b = new BlankNodeID("table.col1,table.col2", "classmap1");
		NodeMaker maker = new TypedNodeMaker(TypedNodeMaker.BLANK, b, true);
		assertEquals("Blank(BlankNodeID(table.col1,table.col2))", 
				maker.toString());
	}
	
	public void testPlainLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new Column("foo.bar"), true);
		assertEquals("Literal(Column(foo.bar))", l.toString());
	}
	
	public void testLanguageLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.languageLiteral("en"),
				new Column("foo.bar"), true);
		assertEquals("Literal@en(Column(foo.bar))", l.toString());
	}
	
	public void testTypedLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.typedLiteral(XSDDatatype.XSDstring),
				new Column("foo.bar"), true);
		assertEquals("Literal^^xsd:string(Column(foo.bar))", l.toString());
	}
	
	public void testURIMakerToString() {
		NodeMaker u = new TypedNodeMaker(TypedNodeMaker.URI, new Column("foo.bar"), true);
		assertEquals("URI(Column(foo.bar))", u.toString());
	}
}
