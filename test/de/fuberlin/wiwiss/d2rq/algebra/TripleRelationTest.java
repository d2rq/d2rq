package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.values.Column;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

public class TripleRelationTest extends TestCase {

	public void testWithPrefix() {
		RelationName original = new RelationName(null, "original");
		RelationName alias = new RelationName(null, "alias");
		AliasMap aliases = AliasMap.create1(original, alias);
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>(Arrays.asList(new Attribute[]{
				new Attribute(original, "id"), 
				new Attribute(alias, "value")}));
		Relation rel = new RelationImpl(
				null, aliases, Expression.TRUE, Expression.TRUE, 
				Collections.<Join>emptySet(), projections, false, OrderSpec.NONE, Relation.NO_LIMIT, Relation.NO_LIMIT);
		TripleRelation t = new TripleRelation(rel, 
				new TypedNodeMaker(TypedNodeMaker.URI, new Pattern("http://example.org/original/@@original.id@@"), true),
				new FixedNodeMaker(Node.createURI("http://example.org/property"), false),
				new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new Column(new Attribute(alias, "value")), false));
		assertEquals("URI(Pattern(http://example.org/original/@@original.id@@))", 
				t.nodeMaker(TripleRelation.SUBJECT).toString());
		assertEquals("Literal(Column(alias.value))", 
				t.nodeMaker(TripleRelation.OBJECT).toString());
		assertEquals("AliasMap(original AS alias)", 
				t.baseRelation().aliases().toString());
		NodeRelation t4 = t.withPrefix(4);
		assertEquals("URI(Pattern(http://example.org/original/@@T4_original.id@@))", 
				t4.nodeMaker(TripleRelation.SUBJECT).toString());
		assertEquals("Literal(Column(T4_alias.value))", 
				t4.nodeMaker(TripleRelation.OBJECT).toString());
		assertEquals("AliasMap(original AS T4_alias, original AS T4_original)", 
				t4.baseRelation().aliases().toString());
	}
}
