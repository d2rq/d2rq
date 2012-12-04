package org.d2rq.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.db.ResultRow;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.ProjectionSpec.ColumnProjectionSpec;
import org.d2rq.db.schema.ColumnName;
import org.junit.Test;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ResultRowTest {
	private static final ProjectionSpec col1 = 
			ColumnProjectionSpec.create(ColumnName.parse("foo.col1"));
	private static final ProjectionSpec col2 = 
			ColumnProjectionSpec.create(ColumnName.parse("foo.col2"));
	
	@Test
	public void testGetUndefinedReturnsNull() {
		ResultRow r = new ResultRow(Collections.<ProjectionSpec,String>emptyMap());
		assertNull(r.get(col1));
		assertNull(r.get(col2));
	}
	
	@Test
	public void testGetColumnReturnsValue() {
		Map<ProjectionSpec,String> m = new HashMap<ProjectionSpec,String>();
		m.put(col1, "value1");
		ResultRow r = new ResultRow(m);
		assertEquals("value1", r.get(col1));
		assertNull(r.get(col2));
	}
	
	@Test
	public void testEmptyRowToString() {
		assertEquals("{}", new ResultRow(Collections.<ProjectionSpec,String>emptyMap()).toString());
	}
	
	@Test
	public void testTwoItemsToString() {
		Map<ProjectionSpec,String> m = new HashMap<ProjectionSpec,String>();
		m.put(col1, "value1");
		m.put(col2, "value2");
		// columns sorted alphabetically
		assertEquals("{foo.col1 => 'value1', foo.col2 => 'value2'}", new ResultRow(m).toString());
	}
}
