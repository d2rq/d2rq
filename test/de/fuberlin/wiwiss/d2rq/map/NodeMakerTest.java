package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

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
	
	public void testBlankNodeIDToString() {
		BlankNodeIdentifier b = new BlankNodeIdentifier("table.col1,table.col2", "classmap1");
		assertEquals("BlankNodeID(Column(table.col1),Column(table.col2))", b.toString());
	}
	
	public void testBlankNodeMakerToString() {
		BlankNodeIdentifier b = new BlankNodeIdentifier("table.col1,table.col2", "classmap1");
		BlankNodeMaker maker = new BlankNodeMaker(b, true);
		assertEquals("Blank(BlankNodeID(Column(table.col1),Column(table.col2)))", 
				maker.toString());
	}
	
	public void testConditionNodeMakerToString() {
		ConditionNodeMaker c = new ConditionNodeMaker(
				new FixedNodeMaker(Node.createURI("http://example.org/")),
				new Expression("1+1=2"));
		assertEquals("Condition(Fixed(<http://example.org/>) WHERE 1+1=2)", c.toString());
	}
	
	public void testPlainLiteralMakerToString() {
		LiteralMaker l = new LiteralMaker(new Column("foo.bar"), true, null, null);
		assertEquals("Literal(Column(foo.bar))", l.toString());
	}
	
	public void testLanguageLiteralMakerToString() {
		LiteralMaker l = new LiteralMaker(new Column("foo.bar"), true, null, "en");
		assertEquals("Literal(Column(foo.bar)@en)", l.toString());
	}
	
	public void testTypedLiteralMakerToString() {
		LiteralMaker l = new LiteralMaker(new Column("foo.bar"), true, XSDDatatype.XSDstring, null);
		assertEquals("Literal(Column(foo.bar)^^xsd:string)", l.toString());
	}
	
	public void testURIMakerToString() {
		UriMaker u = new UriMaker(new Column("foo.bar"), true);
		assertEquals("URI(Column(foo.bar))", u.toString());
	}
}
