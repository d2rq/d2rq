package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamerMap;

public class JoinTest extends TestCase {

	public void testToString() {
		Join join = new Join();
		join.addCondition(new Attribute("table1.foo"), new Attribute("table2.foo"));
		assertEquals("Join(table1.foo = table2.foo)", join.toString());
	}
	
	public void testRenameColumns() {
		Map map = new HashMap();
		map.put(new Attribute("foo.col1"), new Attribute("foo.col2"));
		Join join = new Join();
		join.addCondition(new Attribute("foo.col1"), new Attribute("bar.col1"));
		assertEquals("Join(foo.col2 = bar.col1)", join.renameColumns(new ColumnRenamerMap(map)).toString());
	}
}
