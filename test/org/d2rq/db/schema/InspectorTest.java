package org.d2rq.db.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.HSQLDatabase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InspectorTest {
	private static HSQLDatabase db;
	private Inspector inspector;
	private TableName t1;
	private Identifier col1, col2;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		db = new HSQLDatabase("test");
		db.executeSQL("CREATE TABLE T1 (COL1 INT NOT NULL, COL2 INT NULL)");
	}
	
	@Before
	public void setUp() {
		inspector = new Inspector(db.getConnection(), db.vendor());
		t1 = TableName.parse("T1");
		col1 = Identifier.createUndelimited("COL1");
		col2 = Identifier.createUndelimited("COL2");
	}
	
	@AfterClass
	public static void tearDownClass() {
		db.close(true);
	}
	
	@Test
	public void testRecognizeNullableColumn() {
		assertTrue(inspector.describeTableOrView(t1).getColumnDef(col2).isNullable());
	}

	@Test
	public void testRecognizeNonNullableColumn() {
		assertFalse(inspector.describeTableOrView(t1).getColumnDef(col1).isNullable());
	}
}
