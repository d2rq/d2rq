package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.TableName;
import org.d2rq.lang.AliasDeclaration;
import org.junit.Before;
import org.junit.Test;

public class AliasDeclarationTest {
	private TableName t1, t2, t3;
	private AliasDeclaration alias12, alias12b, alias13, alias32;
	
	@Before
	public void setUp() throws Exception {
		t1 = TableName.parse("t1");
		t2 = TableName.parse("t2");
		t3 = TableName.parse("t3");
		alias12 = new AliasDeclaration(t1, t2);
		alias12b = new AliasDeclaration(t1, t2);
		alias13 = new AliasDeclaration(t1, t3);
		alias32 = new AliasDeclaration(t3, t2);
	}

	@Test
	public void testGetOriginal() {
		assertEquals(t1, alias12.getOriginal());
	}
	
	@Test
	public void testGetAlias() {
		assertEquals(t2, alias12.getAlias());
	}
	
	@Test
	public void testToString() {
		assertEquals("t1 AS t2", alias12.toString());
	}
	
	@Test
	public void testEqualitySame() {
		assertTrue(alias12.equals(alias12b));
		assertEquals(alias12.hashCode(), alias12b.hashCode());
	}
	
	@Test
	public void testEqualityDifferentOriginal() {
		assertFalse(alias12.equals(alias32));
		assertFalse(alias12.hashCode() == alias32.hashCode());
	}
	
	@Test
	public void testEqualityDifferentAlias() {
		assertFalse(alias12.equals(alias13));
		assertFalse(alias12.hashCode() == alias13.hashCode());
	}
	
	@Test
	public void testRenamer() {
		Renamer renamer = AliasDeclaration.getRenamer(Collections.singleton(alias12));
		assertEquals(t2, renamer.applyTo(t1));
		assertEquals(t2, renamer.applyTo(t2));
		assertEquals(t3, renamer.applyTo(t3));
	}
}
