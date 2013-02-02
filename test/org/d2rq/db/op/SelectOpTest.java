package org.d2rq.db.op;

import static org.junit.Assert.assertEquals;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.junit.Test;

public class SelectOpTest {
	
	@Test
	public void testSelectFalseIsEmptyRelation() {
		DatabaseOp table = DummyDB.createTable("foo", "bar");
		assertEquals(EmptyOp.create(table), SelectOp.select(table, Expression.FALSE));
	}

	@Test
	public void testSelectFalseOnNoColumnsIsEmptyTrue() {
		DatabaseOp table = DummyDB.createTable("foo", "bar");
		table = ProjectOp.project(table, new ColumnName[]{});
		assertEquals(EmptyOp.NO_COLUMNS, SelectOp.select(table, Expression.FALSE));
	}
}
