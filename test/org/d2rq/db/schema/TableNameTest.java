package org.d2rq.db.schema;

import static org.d2rq.db.schema.TableName.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.schema.TableName;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TableNameTest {
	private TableName table1, table1b, table2, xTable1, xTable1b, xTable2, yTable1;

	@Before
	public void setUp() {
		table1 = parse("table1");
		table1b = parse("table1");
		table2 = parse("table2");
		xTable1 = parse("x.table1");
		xTable1b = parse("x.table1");
		xTable2 = parse("x.table2");
		yTable1 = parse("y.table1");
	}
	
	@Test
	public void testWithoutSchema() {
		assertEquals("table1", table1.getTable().getName());
		assertNull(table1.getSchema());
	}
	
	@Test
	public void testWithSchema() {
		assertEquals("table1", xTable1.getTable().getName());
		assertEquals("x", xTable1.getSchema().getName());
	}
	
	@Test
	public void testToString() {
		assertEquals("table1", table1.toString());
		assertEquals("x.table1", xTable1.toString());
	}
	
	@Test
	public void testSameNameIsEqual() {
		assertEquals(table1, table1b);
		assertEquals(table1b, table1);
		assertEquals(table1.hashCode(), table1b.hashCode());
	}
	
	@Test
	public void testDifferentNamesAreNotEqual() {
		assertFalse(table1.equals(table2));
		assertFalse(table2.equals(table1));
		assertFalse(table1.hashCode() == table2.hashCode());
	}
	
	@Test
	public void testSameTableAndSchemaNameIsEqual() {
		assertEquals(xTable1, xTable1b);
		assertEquals(xTable1b, xTable1);
		assertEquals(xTable1.hashCode(), xTable1b.hashCode());
	}
	
	@Test
	public void testDifferentSchemaNamesAreNotEqual() {
		assertFalse(xTable1.equals(yTable1));
		assertFalse(yTable1.equals(xTable1));
		assertFalse(xTable1.hashCode() == yTable1.hashCode());
	}
	
	@Test
	public void testSchemaAndNoSchemaAreNotEqual() {
		assertFalse(xTable1.equals(table1));
		assertFalse(table1.equals(xTable1));
		assertFalse(table1.hashCode() == xTable1.hashCode());
	}
	
	@Test
	public void testCompareNamesDifferentSchema() {
		assertTrue(xTable1.compareTo(yTable1) < 0);
		assertTrue(yTable1.compareTo(xTable1) > 0);
	}
	
	@Test
	public void testCompareNamesSameSchema() {
		assertTrue(table1.compareTo(table2) < 0);
		assertTrue(table2.compareTo(table1) > 0);
		assertTrue(xTable1.compareTo(xTable2) < 0);
		assertTrue(xTable2.compareTo(xTable1) > 0);
	}
	
	@Test
	public void testNoSchemaNameSmallerSchemaName() {
		TableName noSchema = parse("z");
		TableName schema = parse("schema.a");
		assertTrue(noSchema.compareTo(schema) < 0);
		assertTrue(schema.compareTo(noSchema) > 0);
	}
	
	@Test
	public void testCompareSameName() {
		assertEquals(0, table1.compareTo(table1));
		assertEquals(0, xTable1.compareTo(xTable1));
	}

	@Test
	public void testNameWithPrefixNoSchema() {
		assertEquals("T42_table1",
				TableName.parse("table1").withPrefix(42).getTable().getName());
		assertEquals("T42_table1",
				TableName.parse("\"table1\"").withPrefix(42).getTable().getName());
	}
	
	@Test
	public void testNameWithPrefixWithSchema() {
		assertEquals("T42_x_table1",
				TableName.parse("x.table1").withPrefix(42).getTable().getName());
		assertEquals("T42_x_table1",
				TableName.parse("\"x\".\"table1\"").withPrefix(42).getTable().getName());
	}
	
	@Test
	public void testAbbreviateLongName() {
		String longName = "ORACLE_CANNOT_HANDLE_ALIASES_LONGER_THAN_30_CHARACTERS";
		assertTrue(TableName.parse(longName).withPrefix(1).getTable().getName().length() <= 30);
	}
}
