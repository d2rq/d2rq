package org.d2rq.db.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.DummyDB;
import org.d2rq.db.DummyDB.DummyTable;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.junit.Before;
import org.junit.Test;


public class AliasOpTest {
	private TableName t1, t2;
	private ColumnName col1, t1col1, t2col1;
	private AliasOp alias12, alias12b, alias13, alias23;
	
	@Before
	public void setUp() {
		t1 = TableName.parse("t1");
		t2 = TableName.parse("t2");
		col1 = ColumnName.parse("col1");
		t1col1 = ColumnName.parse("t1.col1");
		t2col1 = ColumnName.parse("t2.col1");
		// Table t1 with column col1, aliased as t2
		alias12 = AliasOp.create(DummyDB.createTable("t1", "col1"), "t2");
		alias12b = AliasOp.create(DummyDB.createTable("t1", "col1"), "t2");
		alias13 = AliasOp.create(DummyDB.createTable("t1", "col1"), "t3");
		alias23 = AliasOp.create(DummyDB.createTable("t2", "col1"), "t3");
	}

	@Test
	public void testName() {
		AliasOp alias = AliasOp.create(DummyDB.createTable("t1"), t2);
		assertEquals(t2, alias.getTableName());
		assertEquals(t1, alias.getOriginal().getTableName());
	}
	
	@Test
	public void testColumnIsRenamed() {
		assertEquals(t2col1, alias12.getColumns().iterator().next());
	}
	
	@Test
	public void testHasColumn() {
		assertTrue(alias12.hasColumn(col1));
		assertTrue(alias12.hasColumn(t2col1));
		assertFalse(alias12.hasColumn(t1col1));
	}
	
	@Test
	public void testGetColumnType() {
		assertNotNull(alias12.getColumnType(col1));
		assertNotNull(alias12.getColumnType(t2col1));
		assertNull(alias12.getColumnType(t1col1));
	}
	
	@Test
	public void testColumnsInKeysAreRenamed() {
		DummyTable t = DummyDB.createTable("table", "col");
		t.setUniqueKey("col");
		AliasOp alias = AliasOp.create(t, "alias");
		assertEquals(ColumnList.create(ColumnName.parse("alias.col")),
				alias.getUniqueKeys().iterator().next());
	}

	@Test
	public void testToString() {
		assertEquals("Alias(Table(t1) AS t2)", alias12.toString());
	}
	
	@Test
	public void testEqualitySame() {
		assertEquals(alias12, alias12b);
		assertEquals(alias12.hashCode(), alias12b.hashCode());
	}
	
	@Test
	public void testEqualityDifferentAlias() {
		assertFalse(alias12.equals(alias13));
		assertFalse(alias12.hashCode() == alias13.hashCode());
	}
	
	@Test
	public void testEqualityDifferentOriginal() {
		assertFalse(alias13.equals(alias23));
		assertFalse(alias13.hashCode() == alias23.hashCode());
	}
	
	@Test
	public void testFlattenNestedAliases() {
		TableOp t1 = DummyDB.createTable("t1");
		AliasOp a1 = AliasOp.create(t1, "a1");
		AliasOp a2 = AliasOp.create(a1, "a2");
		assertEquals(TableName.parse("a2"), a2.getTableName());
		assertFalse(a1.equals(a2.getOriginal()));
		assertEquals(t1, a2.getOriginal());
	}
}
