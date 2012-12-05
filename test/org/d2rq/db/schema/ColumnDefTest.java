package org.d2rq.db.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.junit.Before;
import org.junit.Test;

public class ColumnDefTest {
	private Identifier col1, col2;
	private DataType varchar, integer;

	@Before
	public void setUp() throws Exception {
		col1 = Identifier.createUndelimited("COL1");
		col2 = Identifier.createUndelimited("COL2");
		varchar = GenericType.CHARACTER.dataTypeFor(Vendor.SQL92);
		integer = GenericType.NUMERIC.dataTypeFor(Vendor.SQL92);
	}

	@Test
	public void testEqualitySame() {
		ColumnDef def1a = new ColumnDef(col1, varchar, true);
		ColumnDef def1b = new ColumnDef(col1, varchar, true);
		assertEquals(def1a, def1b);
		assertEquals(def1a.hashCode(), def1b.hashCode());
	}

	@Test
	public void testEqualityDifferentName() {
		ColumnDef def1 = new ColumnDef(col1, varchar, true);
		ColumnDef def2 = new ColumnDef(col2, varchar, true);
		assertFalse(def1.equals(def2));
		assertFalse(def1.hashCode() == def2.hashCode());
	}

	@Test
	public void testEqualityDifferentType() {
		ColumnDef def1 = new ColumnDef(col1, varchar, true);
		ColumnDef def2 = new ColumnDef(col1, integer, true);
		assertFalse(def1.equals(def2));
		assertFalse(def1.hashCode() == def2.hashCode());
	}

	@Test
	public void testEqualityDifferentNullability() {
		ColumnDef def1 = new ColumnDef(col1, varchar, true);
		ColumnDef def2 = new ColumnDef(col1, varchar, false);
		assertFalse(def1.equals(def2));
		assertFalse(def1.hashCode() == def2.hashCode());
	}
}
