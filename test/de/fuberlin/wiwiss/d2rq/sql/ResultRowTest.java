package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.Column;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRowTest.java,v 1.1 2006/09/09 23:25:15 cyganiak Exp $
 */
public class ResultRowTest extends TestCase {
	private static final Column col1 = new Column("foo.col1");
	private static final Column col2 = new Column("foo.col2");
	
	public void testGetUndefinedReturnsNull() {
		ResultRow r = new ResultRowMap(Collections.EMPTY_MAP);
		assertNull(r.get(col1));
		assertNull(r.get(col2));
	}
	
	public void testGetColumnReturnsValue() {
		Map m = new HashMap();
		m.put(col1, "value1");
		ResultRow r = new ResultRowMap(m);
		assertEquals("value1", r.get(col1));
		assertNull(r.get(col2));
	}
	
	public void testEmptyRowToString() {
		assertEquals("{}", new ResultRowMap(Collections.EMPTY_MAP).toString());
	}
	
	public void testTwoItemsToString() {
		Map m = new HashMap();
		m.put(col1, "value1");
		m.put(col2, "value2");
		// columns sorted alphabetically
		assertEquals("{foo.col1 => 'value1', foo.col2 => 'value2'}", new ResultRowMap(m).toString());
	}
}
