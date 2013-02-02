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
		assertEquals(-1, IdentifierList.create(colA).compareTo(IdentifierList.create(colA, colB)));
		assertEquals(-1, IdentifierList.create(colB).compareTo(IdentifierList.create(colA, colB)));
		assertEquals(-1, IdentifierList.create(colA).compareTo(IdentifierList.create(colB, colA)));
		assertEquals(-1, IdentifierList.create(colA).compareTo(IdentifierList.create(colA, colA)));
		assertEquals(1, IdentifierList.create(colA, colB).compareTo(IdentifierList.create(colA)));
		assertEquals(1, IdentifierList.create(colA, colB).compareTo(IdentifierList.create(colB)));
		assertEquals(1, IdentifierList.create(colB, colA).compareTo(IdentifierList.create(colA)));
		assertEquals(1, IdentifierList.create(colA, colA).compareTo(IdentifierList.create(colA)));
	}

	@Test
	public void testCompareLength1() {
		assertEquals(0, IdentifierList.create(colA).compareTo(IdentifierList.create(colA)));
		assertEquals(-1, IdentifierList.create(colA).compareTo(IdentifierList.create(colB)));
		assertEquals(1, IdentifierList.create(colB).compareTo(IdentifierList.create(colA)));
	}
	
	@Test
	public void testCompareLength2() {
		assertEquals(0, IdentifierList.create(colA, colB).compareTo(IdentifierList.create(colA, colB)));
		assertEquals(0, IdentifierList.create(colB, colA).compareTo(IdentifierList.create(colB, colA)));
		assertEquals(-1, IdentifierList.create(colA, colB).compareTo(IdentifierList.create(colB, colA)));
		assertEquals(-1, IdentifierList.create(colA, colA).compareTo(IdentifierList.create(colA, colB)));
		assertEquals(1, IdentifierList.create(colB, colA).compareTo(IdentifierList.create(colA, colB)));
		assertEquals(1, IdentifierList.create(colA, colB).compareTo(IdentifierList.create(colA, colA)));
	}
	
	@Test
	public void testCompareHasIdentifierSemantics() {
		assertEquals(0, 
				IdentifierList.create(Identifier.createUndelimited("aaa")).compareTo(
						IdentifierList.create(Identifier.createUndelimited("AAA"))));
	}
}
