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
 * @version $Id: AttributeTest.java,v 1.4 2006/09/14 16:22:48 cyganiak Exp $
 */
public class AttributeTest extends TestCase {
	private final static RelationName table1 = new RelationName(null, "table1");
	private final static RelationName table1b = new RelationName(null, "table1");
	private final static RelationName table2 = new RelationName(null, "table2");
	private final static RelationName xTable1 = new RelationName("x", "table1");
	private final static RelationName xTable2 = new RelationName("x", "table2");
	private final static RelationName yTable1 = new RelationName("y", "table1");

	private final static Attribute fooCol1 = new Attribute(null, "foo", "col1");
	private final static Attribute fooCol2 = new Attribute(null, "foo", "col2");
	private final static Attribute barCol1 = new Attribute(null, "bar", "col1");
	private final static Attribute barCol2 = new Attribute(null, "bar", "col2");
	
	public void testCreateNewAttribute() {
		new Attribute("table.column");
	}
	
	public void testInvalidAttributeName() {
		try {
			new Attribute("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException d2rqex) {
			// expected
		}
	}

	public void testAttributeNames() {
		Attribute col = new Attribute("table.column");
		assertEquals("table.column", col.qualifiedName());
		assertNull(col.schemaName());
		assertEquals("table", col.tableName());
		assertEquals("column", col.attributeName());
		assertEquals(new RelationName(null, "table"), 
				col.relationName());
	}

	public void testAttributeNameWithSchema() {
		Attribute column = new Attribute("schema.table.column");
		assertEquals("schema.table.column", column.qualifiedName());
		assertEquals("schema", column.schemaName());
		assertEquals("table", column.tableName());
		assertEquals("column", column.attributeName());
		assertEquals(new RelationName("schema", "table"), 
				column.relationName());
	}

	public void testAttributeEquality() {
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
	
	public void testAttributeEqualityWithSchema() {
		Attribute schema0 = new Attribute("table.column");
		Attribute schema1 = new Attribute("schema1.table.column");
		Attribute schema2 = new Attribute("schema2.table.column");
		Attribute schema2b = new Attribute("schema2.table.column");
		assertFalse(schema0.equals(schema1));
		assertFalse(schema1.equals(schema2));
		assertTrue(schema2.equals(schema2b));
	}
	
	public void testAttributeHashCode() {
		Map map = new HashMap();
		Attribute col1 = new Attribute("table.col1");
		Attribute col1b = new Attribute("table.col1");
		Attribute col2 = new Attribute("table.col2");
		Attribute col3 = new Attribute("table.col3");
		Attribute col1schema = new Attribute("schema.table.col1");
		map.put(col1, "foo");
		map.put(col2, "");
		map.put(col1schema, "bar");
		assertEquals("foo", map.get(col1));
		assertEquals("foo", map.get(col1b));
		assertEquals("", map.get(col2));
		assertNull(map.get(col3));
		assertEquals("bar", map.get(col1schema));
	}
	
	public void testFindColumnInEmptyExpression() {
		assertEquals(Collections.EMPTY_SET, Attribute.findColumnsInExpression("1+2"));
	}
	
	public void testNumbersInExpressionsAreNotColumns() {
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
	
	public void testFindColumnsInExpressionWithSchema() {
		assertEquals(new HashSet(Arrays.asList(new Attribute[]{new Attribute("s1.t1.c1"), new Attribute("s2.t2.c2")})),
				Attribute.findColumnsInExpression("s1.t1.c1 + s2.t2.c2 = 135"));
	}
	
	public void testReplaceColumnsInExpressionWithAliasMap() {
		Map map = new HashMap();
		map.put(new RelationName(null, "bar"), new RelationName(null, "foo"));
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
	
	public void testReplaceColumnsWithSchemaInExpressionWithAliasMap() {
		Map map = new HashMap();
		map.put(new RelationName("schema", "bar"), new RelationName("schema", "foo"));
		AliasMap fooAsBar = new AliasMap(map);
		assertEquals("schema.bar.col1", 
				Attribute.replaceColumnsInExpression("schema.foo.col1", fooAsBar));
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
	
	public void testAttributeToString() {
		assertEquals("@@foo.bar@@", new Attribute("foo.bar").toString());
		assertEquals("@@schema.foo.bar@@", new Attribute("schema.foo.bar").toString());
	}

	public void testCompareSameAttribute() {
		assertEquals(0, fooCol1.compareTo(fooCol1));
	}
	
	public void testCompareSameTableDifferentAttribute() {
		assertTrue(fooCol1.compareTo(fooCol2) < 0);
		assertTrue(fooCol2.compareTo(fooCol1) > 0);
	}
	
	public void testCompareSameAttributeDifferentTable() {
		assertTrue(barCol1.compareTo(fooCol1) < 0);
		assertTrue(fooCol1.compareTo(barCol2) > 0);
	}
	
	public void testCompareDifferentAttributeDifferentTable() {
		assertTrue(barCol2.compareTo(fooCol1) < 0);
		assertTrue(fooCol1.compareTo(barCol2) > 0);
	}
	
	public void testNoSchemaAttributeSmallerThanSchemaAttribute() {
		Attribute noSchema = new Attribute(null, "z", "col");
		Attribute schema = new Attribute("schema", "a", "col");
		assertTrue(noSchema.compareTo(schema) < 0);
		assertTrue(schema.compareTo(noSchema) > 0);
	}
	
	public void testRelationNameWithoutSchema() {
		RelationName r = new RelationName(null, "table");
		assertEquals("table", r.tableName());
		assertNull(r.schemaName());
		assertEquals("table", r.qualifiedName());
	}
	
	public void testRelationNameWithSchema() {
		RelationName r = new RelationName("schema", "table");
		assertEquals("table", r.tableName());
		assertEquals("schema", r.schemaName());
		assertEquals("schema.table", r.qualifiedName());
	}
	
	public void testRelationNameToString() {
		assertEquals("table", 
				new RelationName(null, "table").toString());
		assertEquals("schema.table",
				new RelationName("schema", "table").toString());
	}
	
	public void testSameRelationNameIsEqual() {
		assertEquals(table1, table1b);
		assertEquals(table1b, table1);
		assertEquals(table1.hashCode(), table1b.hashCode());
	}
	
	public void testDifferentRelationNamesAreNotEqual() {
		assertFalse(table1.equals(table2));
		assertFalse(table2.equals(table1));
		assertFalse(table1.hashCode() == table2.hashCode());
	}
	
	public void testSameRelationAndSchemaNameIsEqual() {
		assertEquals(table1, table1b);
		assertEquals(table1b, table1);
		assertEquals(table1.hashCode(), table1b.hashCode());
	}
	
	public void testDifferentSchemaNamesAreNotEqual() {
		assertFalse(xTable1.equals(yTable1));
		assertFalse(yTable1.equals(xTable1));
		assertFalse(xTable1.hashCode() == yTable1.hashCode());
	}
	
	public void testSchemaAndNoSchemaAreNotEqual() {
		assertFalse(xTable1.equals(table1));
		assertFalse(table1.equals(xTable1));
		assertFalse(table1.hashCode() == xTable1.hashCode());
	}
	
	public void testCompareRelationNamesDifferentSchema() {
		assertTrue(xTable1.compareTo(yTable1) < 0);
		assertTrue(yTable1.compareTo(xTable1) > 0);
	}
	
	public void testCompareRelationNamesSameSchema() {
		assertTrue(table1.compareTo(table2) < 0);
		assertTrue(table2.compareTo(table1) > 0);
		assertTrue(xTable1.compareTo(xTable2) < 0);
		assertTrue(xTable2.compareTo(xTable1) > 0);
	}
	
	public void testNoSchemaRelationNameSmallerSchemaRelationName() {
		RelationName noSchema = new RelationName(null, "z");
		RelationName schema = new RelationName("schema", "a");
		assertTrue(noSchema.compareTo(schema) < 0);
		assertTrue(schema.compareTo(noSchema) > 0);
	}
	
	public void testCompareSameRelationName() {
		assertEquals(0, table1.compareTo(table1));
		assertEquals(0, xTable1.compareTo(xTable1));
	}
}
