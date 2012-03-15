package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.SQL;

public class GraphPatternTranslatorTest extends TestCase {
	private final static RelationName table1 = SQL.parseRelationName("table1");
	private final static Attribute table1id = SQL.parseAttribute("table1.id");
	private final static Attribute t1table1id = SQL.parseAttribute("T1_table1.id");
	private final static Attribute t2table1id = SQL.parseAttribute("T2_table1.id");
	
	public void testEmptyGraphAndBGP() {
		NodeRelation nodeRel = translate1(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
		assertEquals(Relation.TRUE, nodeRel.baseRelation());
		assertEquals(Collections.EMPTY_SET, nodeRel.variableNames());
	}
	
	public void testEmptyGraph() {
		assertNull(translate1("?subject ?predicate ?object", Collections.EMPTY_LIST));
	}
	
	public void testEmptyBGP() {
		NodeRelation nodeRel = translate1(Collections.EMPTY_LIST, "engine/type-bridge.n3");
		assertEquals(Relation.TRUE, nodeRel.baseRelation());
		assertEquals(Collections.EMPTY_SET, nodeRel.variableNames());
	}
	
	public void testAskNoMatch() {
		assertNull(translate1("ex:res1 rdf:type foaf:Project", "engine/type-bridge.n3"));
	}

	public void testAskMatch() {
		NodeRelation nodeRel = translate1("ex:res1 rdf:type ex:Class1", "engine/type-bridge.n3");
		Relation r = nodeRel.baseRelation();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.condition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.EMPTY_SET, nodeRel.variableNames());
	}

	public void testFindNoMatch() {
		assertNull(translate1("ex:res1 ex:foo ?foo", "engine/type-bridge.n3"));
	}
	
	public void testFindFixedMatch() {
		NodeRelation nodeRel = translate1("ex:res1 rdf:type ?type", "engine/type-bridge.n3");
		Relation r = nodeRel.baseRelation();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.condition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton("type"), nodeRel.variableNames());
		assertEquals("Fixed(<http://example.org/Class1>)", 
				nodeRel.nodeMaker("type").toString());
	}
	
	public void testFindMatch() {
		NodeRelation nodeRel = translate1("?r rdf:type ex:Class1", "engine/type-bridge.n3");
		Relation r = nodeRel.baseRelation();
		assertEquals(Collections.singleton(table1), r.tables());
		assertEquals(Collections.singleton(table1id), r.projections());
		assertEquals(Expression.TRUE, r.condition());
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton("r"), nodeRel.variableNames());
		assertEquals("URI(Pattern(http://example.org/res@@table1.id@@))", 
				nodeRel.nodeMaker("r").toString());
	}
	
	public void testConstraintInTripleNoMatch() {
		assertNull(translate1("?x rdf:type ?x", "engine/type-bridge.n3"));
	}
	
	public void testConstraintInTripleMatch() {
		NodeRelation nodeRel = translate1("?x rdf:type ?x", "engine/object-uricolumn.n3");
		Relation r = nodeRel.baseRelation();
		assertEquals(Collections.singleton(table1), r.tables());
		assertTrue(r.condition() instanceof Equality);	// Too lazy to check both sides
		assertEquals(AliasMap.NO_ALIASES, r.aliases());
		assertEquals(Collections.singleton("x"), nodeRel.variableNames());
	}

	public void testReturnMultipleMatchesForSingleTriplePattern() {
		NodeRelation[] rels = translate("?s ?p ?o", "engine/simple.n3");
		assertEquals(2, rels.length);
	}

	public void testMatchOneOfTwoPropertyBridges() {
		NodeRelation nodeRel = translate1(
				"ex:res1 rdf:type ex:Class1",
				"engine/simple.n3");
		Relation r = nodeRel.baseRelation();
		assertEquals(Collections.EMPTY_SET, r.projections());
		assertEquals(Equality.createAttributeValue(table1id, "1"), r.condition());
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
		assertEquals(Collections.singleton("foo"), nodeRel.variableNames());
		assertEquals("Literal(Column(T2_table1.foo))", nodeRel.nodeMaker("foo").toString());
		Relation r = nodeRel.baseRelation();
		assertEquals("Conjunction(" +
				"Equality(" +
						"AttributeExpr(@@T1_table1.id@@), " +
						"Constant(1@T1_table1.id)), " +
				"Equality(" +
						"AttributeExpr(@@T2_table1.id@@), " +
						"Constant(1@T2_table1.id)))", 
				r.condition().toString());
	}
	
	public void testTwoTriplePatternsWithJoinMatch() {
		NodeRelation nodeRel = translate1(
				"?x rdf:type ex:Class1 . ?x ex:foo ?foo",
				"engine/simple.n3");
		assertEquals(2, nodeRel.variableNames().size());
		assertEquals("Literal(Column(T2_table1.foo))", 
				nodeRel.nodeMaker("foo").toString());
		assertEquals("URI(Pattern(http://example.org/res@@T1_table1.id@@))", 
				nodeRel.nodeMaker("x").toString());
		Relation r = nodeRel.baseRelation();
		assertEquals(Equality.createAttributeEquality(t1table1id, t2table1id),
				r.condition());
	}

	private NodeRelation translate1(String pattern, String mappingFile) {
		return translate1(triplesToList(pattern), mappingFile);
	}
	
	private NodeRelation translate1(List triplePatterns, String mappingFile) {
		return translate1(triplePatterns,
				MapFixture.loadPropertyBridges(mappingFile));
	}

	private NodeRelation translate1(String pattern, Collection tripleRelations) {
		return translate1(triplesToList(pattern), tripleRelations);
	}
	
	private NodeRelation translate1(List triplePatterns, Collection tripleRelations) {
		Collection rels = new GraphPatternTranslator(triplePatterns, tripleRelations, true).translate();
		if (rels.isEmpty()) return null;
		assertEquals(1, rels.size());
		return (NodeRelation) rels.iterator().next();
	}

	private NodeRelation[] translate(String pattern, String mappingFile) {
		Collection rels = new GraphPatternTranslator(triplesToList(pattern),
				MapFixture.loadPropertyBridges(mappingFile), true).translate();
		return (NodeRelation[]) rels.toArray(new NodeRelation[rels.size()]);
	}
	
	private List triplesToList(String pattern) {
		List results = new ArrayList();
		String[] parts = pattern.split("\\s+\\.\\s*");
		for (int i = 0; i < parts.length; i++) {
			results.add(NodeCreateUtils.createTriple(MapFixture.prefixes(), parts[i]));
		}
		return results;
	}
}
