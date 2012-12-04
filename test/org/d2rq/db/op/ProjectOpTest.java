package org.d2rq.db.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Before;
import org.junit.Test;


public class ProjectOpTest {
	private DummyDB db;
	private TableOp t1, t2;
	private ColumnName t1_col1, t1_col2, t2_col1, col1;
	
	@Before
	public void setUp() throws Exception {
		db = new DummyDB();
		t1 = db.table("t1", "col1", "col2");
		t2 = db.table("t2", "col1");
		t1_col1 = ColumnName.parse("t1.col1");
		t1_col2 = ColumnName.parse("t1.col2");
		t2_col1 = ColumnName.parse("t2.col1");
		col1 = ColumnName.parse("col1");
	}
	
	@Test
	public void testListColumns() {
		ProjectOp p1 = ProjectOp.create(t1, t1_col1);
		assertEquals(Arrays.asList(new ColumnName[]{t1_col1}), p1.getColumns());
		ProjectOp p2 = ProjectOp.create(t1, t1_col1, t1_col2);
		assertEquals(Arrays.asList(new ColumnName[]{t1_col1, t1_col2}), p2.getColumns());
	}

	@Test
	public void testProjectionRemovesColumns() {
		ProjectOp p1 = ProjectOp.create(t1, t1_col1);
		assertFalse(p1.hasColumn(t1_col2));
	}
	
	@Test
	public void testHasColumnsQualified() {
		ProjectOp p1 = ProjectOp.create(t1, t1_col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(col1));
	}

	@Test
	public void testHasColumnUnqualified() {
		ProjectionSpec expr1 = ProjectionSpec.create(SQLExpression.create("1+1", GenericType.NUMERIC), db.vendor());
		ProjectOp p1 = ProjectOp.create(t1, expr1);
		assertTrue(p1.hasColumn(expr1.getColumn().getUnqualified()));
		assertFalse(p1.hasColumn(ColumnName.create(t1.getTableName(), expr1.getColumn().getColumn())));
	}
	
	@Test
	public void testRecoverQualifierWhenProjectingToUnqualified() {
		ProjectOp p1 = ProjectOp.create(t1, col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(col1));
	}
	
	@Test
	public void testRetainColumnOrder() {
		ProjectOp p1 = ProjectOp.create(t1, t1_col1, t1_col2);
		ProjectOp p2 = ProjectOp.create(t1, t1_col2, t1_col1);
		assertEquals(Arrays.asList(new ColumnName[]{t1_col1, t1_col2}), p1.getColumns());
		assertEquals(Arrays.asList(new ColumnName[]{t1_col2, t1_col1}), p2.getColumns());
	}
	
	@Test
	public void testAmbiguousUnqualified() {
		ProjectOp p1 = ProjectOp.create(
				InnerJoinOp.join(Arrays.asList(new NamedOp[]{t1, t2}), 
						Collections.<ColumnListEquality>emptySet()),
				t1_col1, t2_col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(t2_col1));
		assertFalse(p1.hasColumn(col1));
	}
}
