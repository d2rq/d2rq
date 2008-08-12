package de.fuberlin.wiwiss.d2rq.algebra;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;

public class RelationTest extends TestCase {
	private ConnectedDB db;
	private Relation rel1;
	
	public void setUp() {
		db = new DummyDB();
		rel1 = Relation.createSimpleRelation(db, 
				new Attribute[]{new Attribute(null, "foo", "bar")});
	}
	
	public void testSelectFalseIsEmptyRelation() {
		assertEquals(Relation.EMPTY, rel1.select(Expression.FALSE));
	}
	
	public void testTrueRelationIsTrivial() {
		assertTrue(Relation.TRUE.isTrivial());
	}
	
	public void testConditionWithNoSelectColumnsIsNotTrivial() {
		assertFalse(
				Relation
					.createSimpleRelation(db, new Attribute[]{})
					.select(SQLExpression.create("foo.bar = 1")).isTrivial());
	}

	public void testQueryWithSelectColumnsIsNotTrivial() {
		assertFalse(rel1.isTrivial());
	}
}
