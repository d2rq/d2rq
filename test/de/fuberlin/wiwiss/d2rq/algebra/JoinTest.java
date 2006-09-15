package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class JoinTest extends TestCase {

	public void testToString() {
		Join join = new Join();
		join.addCondition(new Attribute("table1.foo"), new Attribute("table2.foo"));
		assertEquals("Join(table1.foo <=> table2.foo)", join.toString());
	}
	
	public void testToStringSmallerTableFirst() {
		Join join = new Join();
		join.addCondition(new Attribute("table2.foo"), new Attribute("table1.foo"));
		assertEquals("Join(table1.foo <=> table2.foo)", join.toString());
	}
	
	public void testToStringSmallerAttributeFirst() {
		Join join = new Join();
		join.addCondition(new Attribute("table1.foo"), new Attribute("table2.col1"));
		join.addCondition(new Attribute("table1.bar"), new Attribute("table2.col2"));
		assertEquals("Join(table1.bar, table1.foo <=> table2.col2, table2.col1)", join.toString());
	}
	
	public void testRenameColumns() {
		Map map = new HashMap();
		map.put(new Attribute("foo.col1"), new Attribute("foo.col2"));
		Join join = new Join();
		join.addCondition(new Attribute("foo.col1"), new Attribute("bar.col1"));
		assertEquals("Join(bar.col1 <=> foo.col2)", join.renameColumns(new ColumnRenamerMap(map)).toString());
	}
}
