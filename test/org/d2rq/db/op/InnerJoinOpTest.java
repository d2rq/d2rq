package org.d2rq.db.op;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.DatabaseOp;
import org.junit.Before;
import org.junit.Test;


public class InnerJoinOpTest {
	private DummyDB db;
	private TableOp t1, t2;
//	private ColumnName t1col1, t2col1;
//	private Set<Join> t1t2;
	
	@Before
	public void setUp() throws Exception {
		db = new DummyDB();
		t1 = db.table("t1", "col1");
		t2 = db.table("t2", "col1");
//		t1col1 = ColumnName.parse("t1.col1");
//		t2col1 = ColumnName.parse("t2.col1");
//		t1t2 = Collections.singleton(new Join(t1col1, t2col1, Join.Direction.UNDIRECTED));
	}

	@Test
	public void testOperandOrdering() {
		DatabaseOp j1 = InnerJoinOp.join(Arrays.asList(new NamedOp[]{t1, t2}), Collections.<ColumnListEquality>emptySet());
		assertEquals("InnerJoin([Table(t1), Table(t2)])", j1.toString());
		DatabaseOp j2 = InnerJoinOp.join(Arrays.asList(new NamedOp[]{t2, t1}), Collections.<ColumnListEquality>emptySet());
		assertEquals("InnerJoin([Table(t1), Table(t2)])", j2.toString());
	}
}
