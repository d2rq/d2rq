package org.d2rq.db.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.d2rq.HSQLDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InspectorTest {
	private HSQLDatabase db;
	private Inspector inspector;
	private TableName t1, t2;
	private Identifier col1, col2;
	
	@Before
	public void setUp() {
		db = new HSQLDatabase("test");
		inspector = new Inspector(db.getConnection(), db.vendor());
		t1 = TableName.parse("T1");
		t2 = TableName.parse("T2");
		col1 = Identifier.createUndelimited("COL1");
		col2 = Identifier.createUndelimited("COL2");
	}
	
	@After
	public void tearDown() {
		db.close(true);
	}
	
	@Test
	public void testListColumns() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT, COL2 INT)");
		assertEquals(Arrays.asList(new Identifier[]{col1, col2}),
				inspector.describeTableOrView(t1).getColumnNames());
	}
	
	@Test
	public void testRecognizeNullableColumn() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT NOT NULL, COL2 INT NULL)");
		assertTrue(inspector.describeTableOrView(t1).getColumnDef(col2).isNullable());
	}

	@Test
	public void testRecognizeNonNullableColumn() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT NOT NULL, COL2 INT NULL)");
		assertFalse(inspector.describeTableOrView(t1).getColumnDef(col1).isNullable());
	}
	
	@Test
	public void testGetPrimaryKey() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT PRIMARY KEY)");
		assertEquals(IdentifierList.create(col1), inspector.describeTableOrView(t1).getPrimaryKey());
	}
	
	@Test
	public void testGetUniqueKey() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT, UNIQUE (COL1))");
		Set<IdentifierList> uks = inspector.describeTableOrView(t1).getUniqueKeys();
		assertEquals(1, uks.size());
		assertEquals(IdentifierList.create(col1), uks.iterator().next());
	}
	
	@Test
	public void testGetForeignKey() {
		db.executeSQL("CREATE TABLE T1 (COL1 INT PRIMARY KEY)");
		db.executeSQL("CREATE TABLE T2 (COL2 INT, FOREIGN KEY (COL2) REFERENCES T1 (COL1))");
		Set<ForeignKey> fks = inspector.describeTableOrView(t2).getForeignKeys();
		assertEquals(1, fks.size());
		ForeignKey fk = fks.iterator().next();
		assertEquals(IdentifierList.create(col2), fk.getLocalColumns());
		assertEquals(t1, fk.getReferencedTable());
		assertEquals(IdentifierList.create(col1), fk.getReferencedColumns());
	}
}
