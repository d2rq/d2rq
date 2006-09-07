package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.TestCase;

public class JoinTest extends TestCase {

	public void testToString() {
		Join join = new Join();
		join.addCondition(new Column("table1.foo"), new Column("table2.foo"));
		assertEquals("Join(table1.foo = table2.foo)", join.toString());
	}
}
