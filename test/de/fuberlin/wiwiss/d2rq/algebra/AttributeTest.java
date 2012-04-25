package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit test cases for {@link Attribute}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
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
	
	public void testAttributeNames() {
		Attribute col = new Attribute(null, "table", "column");
		assertEquals("table.column", col.qualifiedName());
		assertNull(col.schemaName());
		assertEquals("table", col.tableName());
		assertEquals("column", col.attributeName());
		assertEquals(new RelationName(null, "table"), 
				col.relationName());
	}

	public void testAttributeNameWithSchema() {
		Attribute column = new Attribute("schema", "table", "column");
		assertEquals("schema.table.column", column.qualifiedName());
		assertEquals("schema", column.schemaName());
		assertEquals("table", column.tableName());
		assertEquals("column", column.attributeName());
		assertEquals(new RelationName("schema", "table"), 
				column.relationName());
	}

	public void testAttributeEquality() {
		Attribute col1 = new Attribute(null, "table", "col1");
		Attribute col1b = new Attribute(null, "table", "col1");
		Attribute col2 = new Attribute(null, "table", "col2");
		Attribute col3 = new Attribute(null, "table2", "col1");
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
		Attribute schema0 = new Attribute(null, "table", "column");
		Attribute schema1 = new Attribute("schema1", "table", "column");
		Attribute schema2 = new Attribute("schema2", "table", "column");
		Attribute schema2b = new Attribute("schema2", "table", "column");
		assertFalse(schema0.equals(schema1));
		assertFalse(schema1.equals(schema2));
		assertTrue(schema2.equals(schema2b));
	}
	
	public void testAttributeHashCode() {
		Map<Attribute,String> map = new HashMap<Attribute,String>();
		Attribute col1 = new Attribute(null, "table", "col1");
		Attribute col1b = new Attribute(null, "table", "col1");
		Attribute col2 = new Attribute(null, "table", "col2");
		Attribute col3 = new Attribute(null, "table", "col3");
		Attribute col1schema = new Attribute("schema", "table", "col1");
		map.put(col1, "foo");
		map.put(col2, "");
		map.put(col1schema, "bar");
		assertEquals("foo", map.get(col1));
		assertEquals("foo", map.get(col1b));
		assertEquals("", map.get(col2));
		assertNull(map.get(col3));
		assertEquals("bar", map.get(col1schema));
	}
	
	public void testAttributeToString() {
		assertEquals("@@foo.bar@@", new Attribute(null, "foo", "bar").toString());
		assertEquals("@@schema.foo.bar@@", new Attribute("schema", "foo", "bar").toString());
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
	
	public void testRelationNameWithPrefixNoSchema() {
		assertEquals("T42_table1", table1.withPrefix(42).qualifiedName());
	}
	
	public void testRelationNameWithPrefixWithSchema() {
		assertEquals("T42_x_table1", xTable1.withPrefix(42).qualifiedName());
	}
}
