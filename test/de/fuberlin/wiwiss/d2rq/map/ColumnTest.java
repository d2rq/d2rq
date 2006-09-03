package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashMap;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.Column;

import junit.framework.TestCase;

/**
 * Unit test cases for {@link Column}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnTest.java,v 1.1 2006/09/03 12:57:30 cyganiak Exp $
 */
public class ColumnTest extends TestCase {

	public void testCreation() {
		new Column("table.column");
		try {
			new Column("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException d2rqex) {
			// expected
		}
	}

	public void testGetNames() {
		Column col = new Column("table.column");
		assertEquals("table.column", col.getQualifiedName());
		assertEquals("table", col.getTableName());
		assertEquals("column", col.getColumnName());
	}

	public void testGetValue() {
		Column col = new Column("table.col1");
		Map map = new HashMap();
		map.put("table.col2", new Integer(0));
		map.put("table.col1", new Integer(1));
		String[] row = {"foo", "bar", "baz"};
		assertEquals("bar", col.getValue(row, map));
	}

	public void testEquals() {
		Column col1 = new Column("table.col1");
		Column col1b = new Column("table.col1");
		Column col2 = new Column("table.col2");
		Column col3 = new Column("table2.col1");
		Integer other = new Integer(42);
		assertFalse(col1.equals(col2));
		assertFalse(col1.equals(col3));
		assertFalse(col1.equals(other));
		assertFalse(col2.equals(col1));
		assertFalse(col3.equals(col1));
		assertFalse(other.equals(col1));
		assertTrue(col1.equals(col1b));
		assertFalse(col1.equals(null));
	}
	
	public void testHashCode() {
		Map map = new HashMap();
		Column col1 = new Column("table.col1");
		Column col1b = new Column("table.col1");
		Column col2 = new Column("table.col2");
		Column col3 = new Column("table.col3");
		map.put(col1, "foo");
		map.put(col2, "");
		assertEquals("foo", map.get(col1));
		assertEquals("foo", map.get(col1b));
		assertEquals("", map.get(col2));
		assertNull(map.get(col3));
	}
}
