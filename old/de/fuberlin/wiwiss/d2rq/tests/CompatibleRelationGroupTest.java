package de.fuberlin.wiwiss.d2rq.tests;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.lang.TabularBuilder;
import org.d2rq.nodes.BindingMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.values.ColumnValueMaker;

import junit.framework.TestCase;

import com.hp.hpl.jena.sparql.core.Var;

import de.fuberlin.wiwiss.d2rq.optimizer.CompatibleRelationGroup;

public class CompatibleRelationGroupTest extends TestCase {
	Set<ProjectionSpec> projections1;
	Set<ProjectionSpec> projections2;
	RelationImpl unique;
	RelationImpl notUnique;
	DummyDB db;
	
	public void setUp() {
		db = new DummyDB();
		Set<NamedOp> tables = Collections.<NamedOp>singleton(DummyDB.table("table"));
		projections1 = AttributeProjectionSpec.createMultiple(new Attribute[]{DummyDB.column(null, "table", "unique")});
		projections2 = AttributeProjectionSpec.createMultiple(new Attribute[]{DummyDB.column(null, "table", "not_unique")});
		unique = new RelationImpl(db, tables, null, null, null, null, 
				projections1, true, null);
		notUnique = new RelationImpl(db, tables, null, null, null, null, 
				projections2, false, null);
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
		Attribute id = DummyDB.column("TABLE.ID");
		db.setIsNullable(id, false);
		NodeMaker x = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new ColumnValueMaker(id));
		Map<Var,NodeMaker> map = Collections.singletonMap(Var.alloc("x"), x);
		BindingMaker bm = new BindingMaker(map, null);
		TabularBuilder b1 = new TabularBuilder(db);
		TabularBuilder b2 = new TabularBuilder(db);
		b1.addProjection(AttributeProjectionSpec.create(id));
		b2.addProjection(AttributeProjectionSpec.create(id));
		b1.addCondition("TABLE.VALUE=1");
		b2.addCondition("TABLE.VALUE=2");
		
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		Relation r1 = b1.getRelation();
		Relation r2 = b2.getRelation();
		group.addBindingMaker(r1, bm);
		assertTrue(group.isCompatible(r2));
		group.addBindingMaker(r2, bm);
		
		assertEquals(2, group.bindingMakers().size());
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r1.getCondition())));
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r2.getCondition())));
		assertEquals(3, group.baseRelation().projections().size());
		assertEquals(r1.getCondition().or(r2.getCondition()), group.baseRelation().getCondition());
		assertEquals(group.bindingMakers().iterator().next().get(Var.alloc("x")), x);
		assertNotNull(group.bindingMakers().iterator().next().getCondition());
	}
	
	public void testCombineConditionAndNoCondition() {
		Attribute id = DummyDB.column("TABLE.ID");
		db.setIsNullable(id, false);
		NodeMaker x = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new ColumnValueMaker(id));
		Map<Var,NodeMaker> map = Collections.singletonMap(Var.alloc("x"), x);
		BindingMaker bm = new BindingMaker(map, null);
		TabularBuilder b1 = new TabularBuilder(db);
		TabularBuilder b2 = new TabularBuilder(db);
		b1.addProjection(AttributeProjectionSpec.create(id));
		b2.addProjection(AttributeProjectionSpec.create(id));
		b1.addCondition("TABLE.VALUE=1");
		
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		Relation r1 = b1.getRelation();
		Relation r2 = b2.getRelation();
		group.addBindingMaker(r1, bm);
		assertTrue(group.isCompatible(r2));
		group.addBindingMaker(r2, bm);
		
		assertEquals(2, group.bindingMakers().size());
		assertTrue(group.baseRelation().projections().contains(new ExpressionProjectionSpec(r1.getCondition())));
		assertEquals(2, group.baseRelation().projections().size());
		assertEquals(Expression.TRUE, group.baseRelation().getCondition());
		Iterator<BindingMaker> it = group.bindingMakers().iterator();
		BindingMaker bm3 = it.next();
		BindingMaker bm4 = it.next();
		assertTrue((bm3.getCondition() == null && bm4.getCondition() != null) 
				|| (bm3.getCondition() != null && bm4.getCondition() == null));
	}
}
