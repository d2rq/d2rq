package org.d2rq.db.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.d2rq.db.renamer.ColumnRenamer;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.junit.Before;
import org.junit.Test;


public class ColumnListEqualityTest {
	private TableName t1, t2;
	private ColumnName t1foo, t2foo, t1bar, t2bar;
	private ColumnListEquality t1foo_t2foo, t2foo_t1foo;
	
	@Before
	public void setUp() {
		t1 = TableName.parse("t1");
		t2 = TableName.parse("t2");
		t1foo = ColumnName.parse("t1.foo");
		t1bar = ColumnName.parse("t1.bar");
		t2foo = ColumnName.parse("t2.foo");
		t2bar = ColumnName.parse("t2.bar");
		t1foo_t2foo = ColumnListEquality.create(t1foo, t2foo);
		t2foo_t1foo = ColumnListEquality.create(t2foo, t1foo);
	}

	@Test
	public void testToString() {
		assertEquals("(t1.foo=t2.foo)", t1foo_t2foo.toString());
	}

	@Test
	public void testToStringSmallerTableGoesFirst() {
		ColumnListEquality join = ColumnListEquality.create(t2foo, t1foo);
		assertEquals("(t1.foo=t2.foo)", join.toString());
	}

	@Test
	public void testToStringRetainsAttributeOrder() {
		ColumnListEquality join = ColumnListEquality.create(t1, Key.create(t1foo, t1bar), t2, Key.create(t2bar, t2foo));
		assertEquals("(t1.foo=t2.bar AND t1.bar=t2.foo)", join.toString());
	}

	@Test
	public void testRenameColumns() {
		Renamer renamer = new ColumnRenamer(Collections.singletonMap(t1foo, t1bar));
		assertEquals("(t1.bar=t2.foo)", renamer.applyTo(t1foo_t2foo).toString());
	}
	
	@Test
	public void testSmallerTableGoesFirst() {
		assertEquals(t1, t1foo_t2foo.getTableName1());
		assertEquals(t1, t2foo_t1foo.getTableName1());
	}
	
	@Test
	public void testTrivialEqual() {
		ColumnListEquality j1 = ColumnListEquality.create(t1foo, t2foo);
		ColumnListEquality j2 = ColumnListEquality.create(t1foo, t2foo);
		assertEquals(j1, j2);
		assertEquals(j2, j1);
		assertEquals(j1.hashCode(), j2.hashCode());
	}

	@Test
	public void testSideOrderDoesNotAffectEquality1() {
		assertEquals(t1foo_t2foo, t2foo_t1foo);
		assertEquals(t2foo_t1foo, t1foo_t2foo);
		assertEquals(t1foo_t2foo.hashCode(), t2foo_t1foo.hashCode());
	}
	
	@Test
	public void testSideOrderDoesNotAffectEquality2() {
		ColumnListEquality j1 = ColumnListEquality.create(t1foo, t2foo);
		ColumnListEquality j2 = ColumnListEquality.create(t2foo, t1foo);
		assertEquals(j1, j2);
		assertEquals(j2, j1);
		assertEquals(j1.hashCode(), j2.hashCode());
	}

	@Test
	public void testDifferentAttributesNotEqual() {
		ColumnListEquality j1 = ColumnListEquality.create(t1foo, t2foo);
		ColumnListEquality j2 = ColumnListEquality.create(t1foo, t2bar);
		assertFalse(j1.equals(j2));
		assertFalse(j2.equals(j1));
		assertFalse(j1.hashCode() == j2.hashCode());
	}
}
