package org.d2rq.db.schema;

import static org.d2rq.db.schema.ColumnName.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnNameTest {
	private ColumnName col, xCol, xColB, yCol, fooCol1, fooCol1b, fooCol2, barCol1, barCol2;
	private ColumnName uq1, uq1b, uq2;
	
	@Before
	public void setUp() {
		col = parse("table.column");
		xCol = parse("x.table.column");
		xColB = parse("x.table.column");
		yCol = parse("y.table.column");
		fooCol1 = parse("foo.col1");
		fooCol1b = parse("foo.col1");
		fooCol2 = parse("foo.col2");
		barCol1 = parse("bar.col1");
		barCol2 = parse("bar.col2");
		uq1 = parse("col1");
		uq1b = parse("col1");
		uq2 = parse("col2");
	}
	
	@Test
	public void testNames() {
		assertEquals("table.column", col.toString());
		assertNull(col.getQualifier().getSchema());
		assertEquals("table", col.getQualifier().getTable().getName());
		assertEquals("column", col.getColumn().getName());
		assertEquals(TableName.parse("table"), col.getQualifier());
	}

	@Test
	public void testNameWithSchema() {
		assertEquals("x.table.column", xCol.toString());
		assertEquals("x", xCol.getQualifier().getSchema().getName());
		assertEquals("table", xCol.getQualifier().getTable().getName());
		assertEquals("column", xCol.getColumn().getName());
		assertEquals(TableName.parse("x.table"), xCol.getQualifier());
	}

	@Test 
	public void testEqualTrivial() {
		assertTrue(fooCol1.equals(fooCol1b));
		assertEquals(fooCol1.hashCode(), fooCol1b.hashCode());
		assertTrue(xCol.equals(xColB));
		assertEquals(xCol.hashCode(), xColB.hashCode());
	}
	
	@Test
	public void testDifferentColumnsNotEqual() {
		assertFalse(fooCol1.equals(fooCol2));
		assertFalse(fooCol2.equals(fooCol1));
		assertFalse(fooCol1.hashCode() == fooCol2.hashCode());
	}
	
	@Test
	public void testDifferentTablesNotEqual() {
		assertFalse(fooCol1.equals(barCol1));
		assertFalse(barCol1.equals(fooCol1));
		assertFalse(fooCol1.hashCode() == barCol1.hashCode());
	}
	
	@Test
	public void testNotEqualNull() {
		assertFalse(fooCol1.equals(null));
	}
	
	@Test
	public void testNotEqualToOtherClass() {
		assertFalse(fooCol1.equals(new Integer(42)));
		assertFalse(new Integer(42).equals(fooCol1));
	}
	
	@Test
	public void testEqualityWithSchema() {
		assertFalse(col.equals(xCol));
		assertFalse(xCol.equals(col));
		assertFalse(xCol.equals(yCol));
		assertTrue(xCol.equals(xColB));
	}
	
	@Test
	public void testEqualityUnqualifiedSame() {
		assertEquals(uq1, uq1b);
		assertEquals(uq1.hashCode(), uq1b.hashCode());
	}
	
	@Test
	public void testEqualityUnqualifiedDifferent() {
		assertFalse(uq1.equals(uq2));
		assertFalse(uq1.hashCode() == uq2.hashCode());
	}
	
	@Test
	public void testQualifiedUnqualifiedDifferent() {
		assertFalse(uq1.equals(fooCol1));
		assertFalse(fooCol1.equals(uq1));
		assertFalse(uq1.hashCode() == fooCol1.hashCode());
	}
	
	@Test
	public void testToString() {
		assertEquals("table.column", col.toString());
		assertEquals("x.table.column", xCol.toString());
	}

	@Test
	public void testCompareSame() {
		assertEquals(0, fooCol1.compareTo(fooCol1));
	}
	
	@Test
	public void testCompareSameTable() {
		assertTrue(fooCol1.compareTo(fooCol2) < 0);
		assertTrue(fooCol2.compareTo(fooCol1) > 0);
	}
	
	@Test
	public void testCompareSameColumnDifferentTable() {
		assertTrue(barCol1.compareTo(fooCol1) < 0);
		assertTrue(fooCol1.compareTo(barCol2) > 0);
	}
	
	@Test
	public void testCompareDifferentColumnDifferentTable() {
		assertTrue(barCol2.compareTo(fooCol1) < 0);
		assertTrue(fooCol1.compareTo(barCol2) > 0);
	}
	
	@Test
	public void testNoSchemaSmallerThanSchema() {
		assertTrue(uq1.compareTo(fooCol1) < 0);
		assertTrue(fooCol1.compareTo(uq1) > 0);
		ColumnName noSchema = parse("z.col");
		ColumnName schema = parse("schema.a.col");
		assertTrue(noSchema.compareTo(schema) < 0);
		assertTrue(schema.compareTo(noSchema) > 0);
	}
	
	@Test
	public void testGetUnqualified() {
		assertFalse(uq1.isQualified());
		assertTrue(fooCol1.isQualified());
		assertEquals(uq1, fooCol1.getUnqualified());
		assertFalse(fooCol1.equals(fooCol1.getUnqualified()));
		assertTrue(uq1.equals(uq1.getUnqualified()));
	}
	
	@Test
	public void testTreeMap() {
		ColumnName fooBar = ColumnName.parse("foo.bar");
		Map<ColumnName,String> test = new TreeMap<ColumnName,String>();
		test.put(fooBar, "x");
		assertTrue(test.containsKey(fooBar));
		assertFalse(test.containsKey(fooBar.getUnqualified()));
		test.put(fooBar.getUnqualified(), "y");
		assertEquals(2, test.size());
	}
}
