package org.d2rq.db.schema;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class KeyTest {
	private Identifier colA, colB;
	
	@Before
	public void setUp() throws Exception {
		colA = Identifier.createDelimited("colA");
		colB = Identifier.createDelimited("colB");
	}

	@Test
	public void testCompareUnequalLength() {
		assertEquals(-1, Key.create(colA).compareTo(Key.create(colA, colB)));
		assertEquals(-1, Key.create(colB).compareTo(Key.create(colA, colB)));
		assertEquals(-1, Key.create(colA).compareTo(Key.create(colB, colA)));
		assertEquals(-1, Key.create(colA).compareTo(Key.create(colA, colA)));
		assertEquals(1, Key.create(colA, colB).compareTo(Key.create(colA)));
		assertEquals(1, Key.create(colA, colB).compareTo(Key.create(colB)));
		assertEquals(1, Key.create(colB, colA).compareTo(Key.create(colA)));
		assertEquals(1, Key.create(colA, colA).compareTo(Key.create(colA)));
	}

	@Test
	public void testCompareLength1() {
		assertEquals(0, Key.create(colA).compareTo(Key.create(colA)));
		assertEquals(-1, Key.create(colA).compareTo(Key.create(colB)));
		assertEquals(1, Key.create(colB).compareTo(Key.create(colA)));
	}
	
	@Test
	public void testCompareLength2() {
		assertEquals(0, Key.create(colA, colB).compareTo(Key.create(colA, colB)));
		assertEquals(0, Key.create(colB, colA).compareTo(Key.create(colB, colA)));
		assertEquals(-1, Key.create(colA, colB).compareTo(Key.create(colB, colA)));
		assertEquals(-1, Key.create(colA, colA).compareTo(Key.create(colA, colB)));
		assertEquals(1, Key.create(colB, colA).compareTo(Key.create(colA, colB)));
		assertEquals(1, Key.create(colA, colB).compareTo(Key.create(colA, colA)));
	}
	
	@Test
	public void testCompareHasIdentifierSemantics() {
		assertEquals(0, 
				Key.create(Identifier.createUndelimited("aaa")).compareTo(
						Key.create(Identifier.createUndelimited("AAA"))));
	}
}
