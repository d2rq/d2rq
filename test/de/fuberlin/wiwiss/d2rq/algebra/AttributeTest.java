package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Unit test cases for {@link Attribute}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AttributeTest.java,v 1.2 2006/09/11 23:22:26 cyganiak Exp $
 */
public class AttributeTest extends TestCase {

	public void testCreation() {
		new Attribute("table.column");
	}
	
	public void testInvalidName() {
		try {
			new Attribute("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException d2rqex) {
			// expected
		}
	}

	public void testGetNames() {
		Attribute col = new Attribute("table.column");
		assertEquals("table.column", col.qualifiedName());
		assertEquals("table", col.tableName());
		assertEquals("column", col.attributeName());
	}

	public void testNameWithTableSpace() {
		Attribute column = new Attribute("space.table.column");
		assertEquals("space.table.column", column.qualifiedName());
		assertEquals("space.table", column.tableName());
		assertEquals("column", column.attributeName());
	}
	
	public void testEquals() {
		Attribute col1 = new Attribute("table.col1");
		Attribute col1b = new Attribute("table.col1");
		Attribute col2 = new Attribute("table.col2");
		Attribute col3 = new Attribute("table2.col1");
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
		Attribute col1 = new Attribute("table.col1");
		Attribute col1b = new Attribute("table.col1");
		Attribute col2 = new Attribute("table.col2");
		Attribute col3 = new Attribute("table.col3");
		map.put(col1, "foo");
		map.put(col2, "");
		assertEquals("foo", map.get(col1));
		assertEquals("foo", map.get(col1b));
		assertEquals("", map.get(col2));
		assertNull(map.get(col3));
	}
	
	public void testFindColumnInEmptyExpression() {
		assertEquals(Collections.EMPTY_SET, Attribute.findColumnsInExpression("1+2"));
		assertEquals(Collections.EMPTY_SET, Attribute.findColumnsInExpression("1.2"));
	}
	
	public void testFindColumnInColumnName() {
		assertEquals(Collections.singleton(new Attribute("t1.c1")),
				Attribute.findColumnsInExpression("t1.c1"));
	}
	
	public void testFindColumnsInExpression() {
		assertEquals(new HashSet(Arrays.asList(new Attribute[]{new Attribute("t1.c1"), new Attribute("t2.c2")})),
				Attribute.findColumnsInExpression("t1.c1 + t2.c2 = 135"));
	}
	
	public void testReplaceColumnsInExpressionWithAliasMap() {
		Map map = new HashMap();
		map.put("bar", "foo");
		AliasMap fooAsBar = new AliasMap(map);
		assertEquals("bar.col1", 
				Attribute.replaceColumnsInExpression("foo.col1", fooAsBar));
		assertEquals("LEN(bar.col1) > 0", 
				Attribute.replaceColumnsInExpression("LEN(foo.col1) > 0", fooAsBar));
		assertEquals("baz.col1", 
				Attribute.replaceColumnsInExpression("baz.col1", fooAsBar));
		assertEquals("fooo.col1", 
				Attribute.replaceColumnsInExpression("fooo.col1", fooAsBar));
		assertEquals("ofoo.col1", 
				Attribute.replaceColumnsInExpression("ofoo.col1", fooAsBar));
	}
	
	public void testReplaceColumnsInExpressionWithColumnReplacer() {
		Map map = new HashMap();
		map.put(new Attribute("foo.col1"), new Attribute("foo.col2"));
		ColumnRenamerMap col1ToCol2 = new ColumnRenamerMap(map);
		assertEquals("foo.col2", 
				Attribute.replaceColumnsInExpression("foo.col1", col1ToCol2));
		assertEquals("LEN(foo.col2) > 0", 
				Attribute.replaceColumnsInExpression("LEN(foo.col1) > 0", col1ToCol2));
		assertEquals("foo.col3", 
				Attribute.replaceColumnsInExpression("foo.col3", col1ToCol2));
		assertEquals("foo.col11", 
				Attribute.replaceColumnsInExpression("foo.col11", col1ToCol2));
		assertEquals("ofoo.col1", 
				Attribute.replaceColumnsInExpression("ofoo.col1", col1ToCol2));
	}
	
	public void testColumnToString() {
		assertEquals("Column(foo.bar)", new Attribute("foo.bar").toString());
	}
}
