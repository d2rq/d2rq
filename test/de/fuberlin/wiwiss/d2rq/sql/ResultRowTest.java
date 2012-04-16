package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ResultRowTest extends TestCase {
	private static final Attribute col1 = new Attribute(null, "foo", "col1");
	private static final Attribute col2 = new Attribute(null, "foo", "col2");
	
	public void testGetUndefinedReturnsNull() {
		ResultRow r = new ResultRowMap(Collections.<ProjectionSpec,String>emptyMap());
		assertNull(r.get(col1));
		assertNull(r.get(col2));
	}
	
	public void testGetColumnReturnsValue() {
		Map<ProjectionSpec,String> m = new HashMap<ProjectionSpec,String>();
		m.put(col1, "value1");
		ResultRow r = new ResultRowMap(m);
		assertEquals("value1", r.get(col1));
		assertNull(r.get(col2));
	}
	
	public void testEmptyRowToString() {
		assertEquals("{}", new ResultRowMap(Collections.<ProjectionSpec,String>emptyMap()).toString());
	}
	
	public void testTwoItemsToString() {
		Map<ProjectionSpec,String> m = new HashMap<ProjectionSpec,String>();
		m.put(col1, "value1");
		m.put(col2, "value2");
		// columns sorted alphabetically
		assertEquals("{@@foo.col1@@ => 'value1', @@foo.col2@@ => 'value2'}", new ResultRowMap(m).toString());
	}
}
