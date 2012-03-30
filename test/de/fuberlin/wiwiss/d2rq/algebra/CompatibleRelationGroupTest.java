package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.sparql.core.Var;

import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.parser.RelationBuilder;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.values.Column;

public class CompatibleRelationGroupTest extends TestCase {
	Set<ProjectionSpec> projections1;
	Set<ProjectionSpec> projections2;
	RelationImpl unique;
	RelationImpl notUnique;
	DummyDB db;
	
	public void setUp() {
		db = new DummyDB();
		projections1 = Collections.<ProjectionSpec>singleton(new Attribute(null, "table", "unique"));
		projections2 = Collections.<ProjectionSpec>singleton(new Attribute(null, "table", "not_unique"));
		unique = new RelationImpl(
				db, AliasMap.NO_ALIASES, Expression.TRUE, Expression.TRUE, 
				Collections.<Join>emptySet(), 
				projections1, true, OrderSpec.NONE, Relation.NO_LIMIT, Relation.NO_LIMIT);
		notUnique = new RelationImpl(
				db, AliasMap.NO_ALIASES, Expression.TRUE, Expression.TRUE, 
				Collections.<Join>emptySet(), 
				projections2, false, OrderSpec.NONE, Relation.NO_LIMIT, Relation.NO_LIMIT);
	}
	
	public void testNotUniqueIsNotCompatible() {
		CompatibleRelationGroup group;
		group = new CompatibleRelationGroup();
		group.addRelation(unique);
		assertTrue(group.isCompatible(unique));
		assertFalse(group.isCompatible(notUnique));
		group = new CompatibleRelationGroup();
		group.addRelation(notUnique);
		assertFalse(group.isCompatible(unique));
	}
	
	public void testNotUniqueIsCompatibleIfSameAttributes() {
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		group.addRelation(notUnique);
		assertTrue(group.isCompatible(notUnique));
	}
	
	public void testCombineDifferentConditions() {
		Attribute id = SQL.parseAttribute("TABLE.ID");
		db.setNullable(id, false);
		NodeMaker x = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new Column(id), true);
		Map<Var,NodeMaker> map = Collections.singletonMap(Var.alloc("x"), x);
		BindingMaker bm = new BindingMaker(map, null);
		RelationBuilder b1 = new RelationBuilder(db);
		RelationBuilder b2 = new RelationBuilder(db);
		b1.addProjection(id);
		b2.addProjection(id);
		b1.addCondition("TABLE.VALUE=1");
		b2.addCondition("TABLE.VALUE=2");
		
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		Relation r1 = b1.buildRelation();
		Relation r2 = b2.buildRelation();
		group.addBindingMaker(r1, bm);
		assertTrue(group.isCompatible(r2));
		group.addBindingMaker(r2, bm);
		
		assertEquals(2, group.bindingMakers().size());
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r1.condition())));
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r2.condition())));
		assertEquals(3, group.baseRelation().projections().size());
		assertEquals(r1.condition().or(r2.condition()), group.baseRelation().condition());
		assertEquals(group.bindingMakers().iterator().next().nodeMaker(Var.alloc("x")), x);
		assertNotNull(group.bindingMakers().iterator().next().condition());
	}
	
	public void testCombineConditionAndNoCondition() {
		Attribute id = SQL.parseAttribute("TABLE.ID");
		db.setNullable(id, false);
		NodeMaker x = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new Column(id), true);
		Map<Var,NodeMaker> map = Collections.singletonMap(Var.alloc("x"), x);
		BindingMaker bm = new BindingMaker(map, null);
		RelationBuilder b1 = new RelationBuilder(db);
		RelationBuilder b2 = new RelationBuilder(db);
		b1.addProjection(id);
		b2.addProjection(id);
		b1.addCondition("TABLE.VALUE=1");
		
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		Relation r1 = b1.buildRelation();
		Relation r2 = b2.buildRelation();
		group.addBindingMaker(r1, bm);
		assertTrue(group.isCompatible(r2));
		group.addBindingMaker(r2, bm);
		
		assertEquals(2, group.bindingMakers().size());
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r1.condition())));
		assertEquals(2, group.baseRelation().projections().size());
		assertEquals(Expression.TRUE, group.baseRelation().condition());
		Iterator<BindingMaker> it = group.bindingMakers().iterator();
		BindingMaker bm3 = it.next();
		BindingMaker bm4 = it.next();
		assertTrue((bm3.condition() == null && bm4.condition() != null) 
				|| (bm3.condition() != null && bm4.condition() == null));
	}
}
