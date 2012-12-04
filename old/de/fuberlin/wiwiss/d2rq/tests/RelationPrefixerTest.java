package org.d2rq.tmp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.algebra.NodeRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.DummyDB;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.schema.Identifier;
import org.d2rq.lang.Pattern;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.values.ColumnValueMaker;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;


public class RelationPrefixerTest extends TestCase {

	public void testWithPrefix() {
		NamedOp original = DummyDB.table("original");
		NamedOp alias = DummyDB.table("alias");
		AliasMap aliases = AliasMap.create1(original, alias.getTableName());
		Set<NamedOp> tables = Collections.singleton(original);
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>(Arrays.asList(new AttributeProjectionSpec[]{
				AttributeProjectionSpec.create(new Attribute(original, original.getTableName(), Identifier.createDelimited("id"))), 
				AttributeProjectionSpec.create(new Attribute(alias, alias.getTableName(), Identifier.createDelimited("value")))}));
		Relation rel = new RelationImpl(null, tables, aliases, null, null, null, 
				projections, false, null);
		TripleRelation t = new TripleRelation(rel, 
				new TypedNodeMaker(TypedNodeMaker.URI, new Pattern("http://example.org/original/@@original.id@@").toTemplate(new DummyDB())),
				new FixedNodeMaker(Node.createURI("http://example.org/property")),
				new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new ColumnValueMaker(new Attribute(alias, null, Identifier.createDelimited("value")))));
		NodeRelation t4 = t.withPrefix(4);
		assertEquals("URI(Pattern(http://example.org/original/@@T4_original.id@@))", 
				t4.nodeMaker(TripleRelation.SUBJECT).toString());
		assertEquals("Literal(Column(T4_alias.value))", 
				t4.nodeMaker(TripleRelation.OBJECT).toString());
		assertEquals("AliasMap(\"original\" AS \"T4_alias\", \"original\" AS \"T4_original\")", 
				t4.getBaseTabular().aliases().toString());
	}
}
