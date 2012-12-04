package de.fuberlin.wiwiss.d2rq.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.d2rq.D2RQTestSuite;
import org.d2rq.algebra.NodeRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.mutators.OpUtil;
import org.d2rq.db.schema.ColumnName;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.util.Context;

import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;


public class GraphPatternTranslatorTest {
	private NamedOp table1;
	private ColumnName table1id, t1table1id, t2table1id;
	private Var foo, type, x;
	
	@Before
	public void setUp() {
		table1 = DummyDB.createTable("table1");
		table1id = ColumnName.parse("table1.id");
		t1table1id = ColumnName.parse("T1_table1.id");
		t2table1id = ColumnName.parse("T2_table1.id");
		foo = Var.alloc("foo");
		type = Var.alloc("type");
		x = Var.alloc("x");
	}

	@Test
	public void testEmptyGraphAndBGP() {
		NodeRelation nodeRel = translate1(Collections.<Triple>emptyList(), Collections.<TripleRelation>emptyList());
		assertTrue(OpUtil.isTrivial(nodeRel.getBaseTabular()));
		assertEquals(Collections.EMPTY_SET, nodeRel.getBindingMaker().variableNames());
	}
	
	@Test
	public void testEmptyGraph() {
		assertNull(translate1("?subject ?predicate ?object", Collections.<TripleRelation>emptyList()));
	}
	
	public void testEmptyBGP() {
		NodeRelation nodeRel = translate1(Collections.<Triple>emptyList(), "engine/type-bridge.n3");
		assertEquals(Relation.TRUE, nodeRel.getBaseTabular());
		assertEquals(Collections.EMPTY_SET, nodeRel.variables());
	}
	
	public void testAskNoMatch() {
		assertNull(translate1("ex:res1 rdf:type foaf:Project", "engine/type-bridge.n3"));
	}

	public void testAskMatch() {
		NodeRelation nodeRel = translate1("ex:res1 rdf:type ex:Class1", "engine/type-bridge.n3");
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.getCondition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.EMPTY_SET, nodeRel.variables());
	}

	public void testFindNoMatch() {
		assertNull(translate1("ex:res1 ex:foo ?foo", "engine/type-bridge.n3"));
	}
	
	public void testFindFixedMatch() {
		NodeRelation nodeRel = translate1("ex:res1 rdf:type ?type", "engine/type-bridge.n3");
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.getCondition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton(type), nodeRel.variables());
		assertEquals("Fixed(<http://example.org/Class1>)", 
				nodeRel.nodeMaker(type).toString());
	}
	
	public void testFindMatch() {
		NodeRelation nodeRel = translate1("?x rdf:type ex:Class1", "engine/type-bridge.n3");
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.singleton(table1id), r.projections());
		assertEquals(Expression.TRUE, r.getCondition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton(x), nodeRel.variables());
		assertEquals("URI(Pattern(http://example.org/res@@table1.id@@))", 
				nodeRel.nodeMaker(x).toString());
	}
	
	public void testConstraintInTripleNoMatch() {
		assertNull(translate1("?x rdf:type ?x", "engine/type-bridge.n3"));
	}
	
	public void testConstraintInTripleMatch() {
		NodeRelation nodeRel = translate1("?x rdf:type ?x", "engine/object-uricolumn.n3");
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Collections.singleton(table1), r.tables());
		assertTrue(r.getCondition() instanceof Equality);	// Too lazy to check both sides
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton(x), nodeRel.variables());
	}

	public void testReturnMultipleMatchesForSingleTriplePattern() {
		NodeRelation[] rels = translate("?s ?p ?o", "engine/simple.n3");
		assertEquals(2, rels.length);
	}

	public void testMatchOneOfTwoPropertyBridges() {
		NodeRelation nodeRel = translate1(
				"ex:res1 rdf:type ex:Class1",
				"engine/simple.n3");
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.getCondition());
	}
	
	public void testAskTwoTriplePatternsNoMatch() {
		assertNull(translate1(
				"ex:res1 rdf:type ex:Class1 . ex:res1 rdf:type ex:Class2",
				"engine/simple.n3"));
	}
	
	public void testAskTwoTriplePatternsMatch() {
		NodeRelation nodeRel = translate1(
				"ex:res1 rdf:type ex:Class1 . ex:res1 ex:foo ?foo",
				"engine/simple.n3");
		assertEquals(Collections.singleton(foo), nodeRel.variables());
		assertEquals("Literal(Column(T2_table1.foo))", nodeRel.nodeMaker(foo).toString());
		Relation r = nodeRel.getBaseTabular();
		assertEquals("Conjunction(" +
				"Equality(" +
						"AttributeExpr(@@T1_table1.id@@), " +
						"Constant(1@T1_table1.id)), " +
				"Equality(" +
						"AttributeExpr(@@T2_table1.id@@), " +
						"Constant(1@T2_table1.id)))", 
				r.getCondition().toString());
	}
	
	public void testTwoTriplePatternsWithJoinMatch() {
		NodeRelation nodeRel = translate1(
				"?x rdf:type ex:Class1 . ?x ex:foo ?foo",
				"engine/simple.n3");
		assertEquals(2, nodeRel.variables().size());
		assertEquals("Literal(Column(T2_table1.foo))", 
				nodeRel.nodeMaker(foo).toString());
		assertEquals("URI(Pattern(http://example.org/res@@T1_table1.id@@))", 
				nodeRel.nodeMaker(x).toString());
		Relation r = nodeRel.getBaseTabular();
		assertEquals(Equality.createColumnEquality(t1table1id, t2table1id),
				r.getCondition());
	}

	private NodeRelation translate1(String pattern, String mappingFile) {
		return translate1(triplesToList(pattern), mappingFile);
	}
	
	private NodeRelation translate1(List<Triple> triplePatterns, String mappingFile) {
		return translate1(triplePatterns,
				D2RQTestSuite.loadPropertyBridges(mappingFile));
	}

	private NodeRelation translate1(String pattern, Collection<TripleRelation> tripleRelations) {
		return translate1(triplesToList(pattern), tripleRelations);
	}
	
	private NodeRelation translate1(List<Triple> triplePatterns, Collection<TripleRelation> tripleRelations) {
		Collection<NodeRelation> rels = new GraphPatternTranslator(triplePatterns, tripleRelations, new Context()).translate();
		if (rels.isEmpty()) return null;
		assertEquals(1, rels.size());
		return (NodeRelation) rels.iterator().next();
	}

	private NodeRelation[] translate(String pattern, String mappingFile) {
		Collection<NodeRelation> rels = new GraphPatternTranslator(triplesToList(pattern),
				D2RQTestSuite.loadPropertyBridges(mappingFile), new Context()).translate();
		return (NodeRelation[]) rels.toArray(new NodeRelation[rels.size()]);
	}
	
	private List<Triple> triplesToList(String pattern) {
		List<Triple> results = new ArrayList<Triple>();
		String[] parts = pattern.split("\\s+\\.\\s*");
		for (int i = 0; i < parts.length; i++) {
			results.add(NodeCreateUtils.createTriple(D2RQTestSuite.STANDARD_PREFIXES, parts[i]));
		}
		return results;
	}
}
