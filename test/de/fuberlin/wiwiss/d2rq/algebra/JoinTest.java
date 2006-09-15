package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;

import junit.framework.TestCase;

public class JoinTest extends TestCase {
	private final static Attribute table1foo = new Attribute(null, "table1", "foo");
	private final static Attribute table1bar = new Attribute(null, "table1", "bar");
	private final static Attribute table2foo = new Attribute(null, "table2", "foo");
	private final static Attribute table2bar = new Attribute(null, "table2", "bar");

	public void testToString() {
		Join join = new Join();
		join.addCondition(table1foo, table2foo);
		assertEquals("Join(table1.foo <=> table2.foo)", join.toString());
	}
	
	public void testToStringSmallerTableFirst() {
		Join join = new Join();
		join.addCondition(table2foo, table1foo);
		assertEquals("Join(table1.foo <=> table2.foo)", join.toString());
	}
	
	public void testToStringSmallerAttributeFirst() {
		Join join = new Join();
		join.addCondition(table1foo, table2foo);
		join.addCondition(table1bar, table2bar);
		assertEquals("Join(table1.bar, table1.foo <=> table2.bar, table2.foo)", join.toString());
	}
	
	public void testRenameColumns() {
		ColumnRenamer renamer = new ColumnRenamerMap(Collections.singletonMap(table1foo, table1bar));
		Join join = new Join();
		join.addCondition(table1foo, table2foo);
		assertEquals("Join(table1.bar <=> table2.foo)", join.renameColumns(renamer).toString());
	}
}
