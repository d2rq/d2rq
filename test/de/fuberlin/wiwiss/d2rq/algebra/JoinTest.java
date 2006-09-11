package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamerMap;

public class JoinTest extends TestCase {

	public void testToString() {
		Join join = new Join();
		join.addCondition(new Column("table1.foo"), new Column("table2.foo"));
		assertEquals("Join(table1.foo = table2.foo)", join.toString());
	}
	
	public void testRenameColumns() {
		Map map = new HashMap();
		map.put(new Column("foo.col1"), new Column("foo.col2"));
		Join join = new Join();
		join.addCondition(new Column("foo.col1"), new Column("bar.col1"));
		assertEquals("Join(foo.col2 = bar.col1)", join.renameColumns(new ColumnRenamerMap(map)).toString());
	}
}
