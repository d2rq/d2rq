package org.d2rq.db.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
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
		ProjectOp p1 = ProjectOp.project(t1, t1_col1);
		assertEquals(ColumnList.create(t1_col1), p1.getColumns());
		ProjectOp p2 = ProjectOp.project(t1, t1_col1, t1_col2);
		assertEquals(ColumnList.create(t1_col1, t1_col2), p2.getColumns());
	}

	@Test
	public void testProjectionRemovesColumns() {
		ProjectOp p1 = ProjectOp.project(t1, t1_col1);
		assertFalse(p1.hasColumn(t1_col2));
	}
	
	@Test
	public void testHasColumnsQualified() {
		ProjectOp p1 = ProjectOp.project(t1, t1_col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(col1));
	}

	@Test
	public void testHasColumnUnqualified() {
		Identifier col = Identifier.createUndelimited("EXPR");
		DatabaseOp p1 = ExtendOp.extend(t1, col, 
				SQLExpression.create("1+1", GenericType.NUMERIC), db.vendor());
		assertTrue(p1.hasColumn(ColumnName.create(col)));
		assertFalse(p1.hasColumn(ColumnName.create(t1.getTableName(), col)));
	}
	
	@Test
	public void testRecoverQualifierWhenProjectingToUnqualified() {
		ProjectOp p1 = ProjectOp.project(t1, col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(col1));
	}
	
	@Test
	public void testRetainColumnOrder() {
		ProjectOp p1 = ProjectOp.project(t1, t1_col1, t1_col2);
		ProjectOp p2 = ProjectOp.project(t1, t1_col2, t1_col1);
		assertEquals(ColumnList.create(t1_col1, t1_col2), p1.getColumns());
		assertEquals(ColumnList.create(t1_col2, t1_col1), p2.getColumns());
	}
	
	@Test
	public void testAmbiguousUnqualified() {
		ProjectOp p1 = ProjectOp.project(
				InnerJoinOp.join(Arrays.asList(new NamedOp[]{t1, t2}), 
						Collections.<ColumnListEquality>emptySet()),
				t1_col1, t2_col1);
		assertTrue(p1.hasColumn(t1_col1));
		assertTrue(p1.hasColumn(t2_col1));
		assertFalse(p1.hasColumn(col1));
	}
}
