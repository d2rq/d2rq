package org.d2rq.nodes;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.values.BlankNodeIDValueMaker;
import org.d2rq.values.ColumnValueMaker;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;


public class NodeMakerTest {
	private ColumnName table_col1, table_col2;
	
	@Before
	public void setUp() {
		table_col1 = ColumnName.parse("table.col1");
		table_col2 = ColumnName.parse("table.col2");		
	}
	
	@Test
	public void testFixedNodeMakerToString() {
		assertEquals("Fixed(\"foo\")", 
				new FixedNodeMaker(Node.createLiteral("foo")).toString());
		assertEquals("Fixed(\"foo\"@en)", 
				new FixedNodeMaker(Node.createLiteral("foo", "en", null)).toString());
		assertEquals("Fixed(\"1\"^^xsd:int)",
				new FixedNodeMaker(Node.createLiteral("1", null, XSDDatatype.XSDint)).toString());
		assertEquals("Fixed(_:foo)", 
				new FixedNodeMaker(Node.createAnon(new AnonId("foo"))).toString());
		assertEquals("Fixed(<http://example.org/>)", 
				new FixedNodeMaker(Node.createURI("http://example.org/")).toString());
	}
	
	@Test
	public void testBlankNodeMakerToString() {
		BlankNodeIDValueMaker b = new BlankNodeIDValueMaker("classmap1", Arrays.asList(new ColumnName[]{table_col1, table_col2}));
		NodeMaker maker = new TypedNodeMaker(TypedNodeMaker.BLANK, b);
		assertEquals("Blank(BlankNodeID(table.col1,table.col2))", 
				maker.toString());
	}
	
	@Test
	public void testPlainLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new ColumnValueMaker(table_col1));
		assertEquals("Literal(Column(table.col1))", l.toString());
	}
	
	@Test
	public void testLanguageLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.languageLiteral("en"), new ColumnValueMaker(table_col1));
		assertEquals("Literal@en(Column(table.col1))", l.toString());
	}
	
	@Test
	public void testTypedLiteralMakerToString() {
		TypedNodeMaker l = new TypedNodeMaker(TypedNodeMaker.typedLiteral(XSDDatatype.XSDstring),
				new ColumnValueMaker(table_col1));
		assertEquals("Literal^^xsd:string(Column(table.col1))", l.toString());
	}
	
	@Test
	public void testURIMakerToString() {
		NodeMaker u = new TypedNodeMaker(TypedNodeMaker.URI, new ColumnValueMaker(table_col1));
		assertEquals("URI(Column(table.col1))", u.toString());
	}
}
