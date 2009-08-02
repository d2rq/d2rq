package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class JoinTest extends TestCase {
	private final static Attribute table1foo = new Attribute(null, "table1", "foo");
	private final static Attribute table1bar = new Attribute(null, "table1", "bar");
	private final static Attribute table2foo = new Attribute(null, "table2", "foo");
	private final static Attribute table2bar = new Attribute(null, "table2", "bar");
	private final static RelationName table1 = new RelationName(null, "table1");
	private final static RelationName table2 = new RelationName(null, "table2");
	
	public void testToString() {
		Join join = new Join(table1foo, table2foo, Join.DIRECTION_UNDIRECTED);
		assertEquals("Join(table1.foo <=> table2.foo)", join.toString());
	}
	
	public void testToStringRetainsTableOrder() {
		Join join = new Join(table2foo, table1foo, Join.DIRECTION_RIGHT);
		assertEquals("Join(table2.foo => table1.foo)", join.toString());
	}
	
	public void testToStringRetainsAttributeOrder() {
		Join join = new Join(
				Arrays.asList(new Attribute[]{table1foo, table1bar}),
				Arrays.asList(new Attribute[]{table2bar, table2foo}), Join.DIRECTION_RIGHT);
		assertEquals("Join(table1.foo, table1.bar => table2.bar, table2.foo)", join.toString());
	}
	
	public void testRenameColumns() {
		ColumnRenamer renamer = new ColumnRenamerMap(Collections.singletonMap(table1foo, table1bar));
		Join join = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		assertEquals("Join(table1.bar => table2.foo)", join.renameColumns(renamer).toString());
	}
	
	public void testTableOrderIsRetained() {
		assertEquals(table1, new Join(table1foo, table2foo, Join.DIRECTION_RIGHT).table1());
		assertEquals(table2, new Join(table2foo, table1foo, Join.DIRECTION_RIGHT).table1());
	}
	
	public void testJoinOverSameAttributesIsEqual() {
		Join j1 = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		Join j2 = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		assertEquals(j1, j2);
		assertEquals(j2, j1);
		assertEquals(j1.hashCode(), j2.hashCode());
	}
	
	public void testSideOrderDoesNotAffectEquality1() {
		Join j1 = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		Join j2 = new Join(table2foo, table1foo, Join.DIRECTION_LEFT);
		assertEquals(j1, j2);
		assertEquals(j2, j1);
		assertEquals(j1.hashCode(), j2.hashCode());
	}
	
	public void testSideOrderDoesNotAffectEquality2() {
		Join j1 = new Join(table1foo, table2foo, Join.DIRECTION_UNDIRECTED);
		Join j2 = new Join(table2foo, table1foo, Join.DIRECTION_UNDIRECTED);
		assertEquals(j1, j2);
		assertEquals(j2, j1);
		assertEquals(j1.hashCode(), j2.hashCode());
	}

	public void testDifferentAttributesNotEqual() {
		Join j1 = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		Join j2 = new Join(table1foo, table2bar, Join.DIRECTION_RIGHT);
		assertFalse(j1.equals(j2));
		assertFalse(j2.equals(j1));
		assertFalse(j1.hashCode() == j2.hashCode());
	}

	public void testDifferentDirectionsNotEqual() {
		Join j1 = new Join(table1foo, table2foo, Join.DIRECTION_RIGHT);
		Join j2 = new Join(table1foo, table2foo, Join.DIRECTION_UNDIRECTED);
		assertFalse(j1.equals(j2));
		assertFalse(j2.equals(j1));
		assertFalse(j1.hashCode() == j2.hashCode());
	}
}
