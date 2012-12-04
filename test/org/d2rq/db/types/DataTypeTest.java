package org.d2rq.db.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;
import org.junit.Before;
import org.junit.Test;


public class DataTypeTest {
	private DataType type1a, type1b, type2, type3;

	@Before
	public void setUp() {
		type1a = DataType.GenericType.CHARACTER.dataTypeFor(Vendor.SQL92);
		type1b = DataType.GenericType.CHARACTER.dataTypeFor(Vendor.SQL92);
		type2 = DataType.GenericType.NUMERIC.dataTypeFor(Vendor.SQL92);
		type3 = DataType.GenericType.CHARACTER.dataTypeFor(Vendor.Oracle);
	}
	
	@Test
	public void testEqualitySame() {
		assertTrue(type1a.equals(type1b));
		assertEquals(type1a.hashCode(), type1b.hashCode());
	}
	
	@Test
	public void testEqualityDifferentType() {
		assertFalse(type1a.equals(type2));
		assertFalse(type1a.hashCode() == type2.hashCode());
	}
	
	@Test
	public void testEqualityDifferentVendor() {
		assertTrue(type1a.equals(type3));
		assertTrue(type1a.hashCode() == type3.hashCode());
	}
}
