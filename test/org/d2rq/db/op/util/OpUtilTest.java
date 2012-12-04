package org.d2rq.db.op.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Test;

public class OpUtilTest {

	@Test
	public void testTrueIsTrivial() {
		assertTrue(OpUtil.isTrivial(DatabaseOp.TRUE));
	}
	
	@Test
	public void testConditionWithNoSelectColumnsIsNotTrivial() {
		assertFalse(OpUtil.isTrivial(SelectOp.select(DatabaseOp.TRUE, 
				SQLExpression.create("foo.bar = 1", GenericType.BOOLEAN))));
	}

	@Test
	public void testQueryWithSelectColumnsIsNotTrivial() {
		assertFalse(OpUtil.isTrivial(DummyDB.createTable("foo", "bar")));
	}
}
