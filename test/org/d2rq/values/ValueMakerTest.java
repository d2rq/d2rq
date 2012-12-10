package org.d2rq.values;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.lang.Microsyntax;
import org.junit.Before;
import org.junit.Test;


public class ValueMakerTest {
	private ColumnName foo_col1, foo_col2;
	
	@Before
	public void setUp() {
		foo_col1 = ColumnName.parse("foo.col1");
		foo_col2 = ColumnName.parse("foo.col2");
	}
	
	@Test
	public void testBlankNodeIDToString() {
		BlankNodeIDValueMaker b = new BlankNodeIDValueMaker("classmap1", Arrays.asList(new ColumnName[]{foo_col1, foo_col2}));
		assertEquals("BlankNodeID(foo.col1,foo.col2)", b.toString());
	}

	@Test
	public void testColumnToString() {
		assertEquals("Column(foo.col1)", new ColumnValueMaker(foo_col1).toString());
	}
	
	@Test
	public void testValueDecoratorWithoutTranslatorToString() {
		assertEquals("Column(foo.col1):maxLength=10",
				new DecoratingValueMaker(
						new ColumnValueMaker(foo_col1), 
						Collections.singletonList(DecoratingValueMaker.maxLengthConstraint(10))).toString());
	}
	
	@Test
	public void testMaxLengthConstraint() {
		DummyValueMaker source = new DummyValueMaker("foo");
		DecoratingValueMaker values = new DecoratingValueMaker(source, Collections.singletonList(DecoratingValueMaker.maxLengthConstraint(5)));
		assertFalse(matches(values, null));
		assertTrue(matches(values, ""));
		assertTrue(matches(values, "foo"));
		assertTrue(matches(values, "fooba"));
		assertFalse(matches(values, "foobar"));
		source.setSelectCondition(Expression.FALSE);
		assertFalse(matches(values, "foo"));
	}
	
	public void testContainsConstraint() {
		DummyValueMaker source = new DummyValueMaker("foo");
		DecoratingValueMaker values = new DecoratingValueMaker(source, Collections.singletonList(DecoratingValueMaker.containsConstraint("foo")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, "foo"));
		assertTrue(matches(values, "barfoobaz"));
		assertFalse(matches(values, ""));
		assertFalse(matches(values, "bar"));
		values = new DecoratingValueMaker(source, Collections.singletonList(DecoratingValueMaker.containsConstraint("")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, ""));
		assertTrue(matches(values, "a"));
		source.setSelectCondition(Expression.FALSE);
		assertFalse(matches(values, "a"));
	}
	
	@Test
	public void testRegexConstraint() {
		DummyValueMaker source = new DummyValueMaker("foo");
		DecoratingValueMaker values = new DecoratingValueMaker(source, Collections.singletonList(DecoratingValueMaker.regexConstraint("^[0-9]{5}$")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, "12345"));
		assertFalse(matches(values, "abc"));
		source.setSelectCondition(Expression.FALSE);
		assertFalse(matches(values, "12345"));
	}
	
	@Test
	public void testColumnDoesNotMatchNull() {
		ColumnValueMaker column = new ColumnValueMaker(foo_col1);
		assertFalse(matches(column, null));
	}
	
	@Test
	public void testBlankNodeIDDoesNotMatchNull() {
		BlankNodeIDValueMaker bNodeID = new BlankNodeIDValueMaker("classmap", Collections.singletonList(foo_col1));
		assertFalse(matches(bNodeID, null));
	}
	
	@Test
	public void testPatternToString() {
		assertEquals(
				"http://test/@@foo.bar@@", 
				Microsyntax.toString(Microsyntax.parsePattern("http://test/@@foo.bar@@")));
	}
	
	@Test
	public void testPatternDoesNotMatchNull() {
		ValueMaker pattern = Microsyntax.parsePattern("foo/@@foo.bar@@");
		assertFalse(matches(pattern, null));
	}
	
	private boolean matches(ValueMaker valueMaker, String value) {
		return !valueMaker.valueExpression(value, DummyDB.createTable("foo"), new DummyDB().vendor()).isFalse();
	}
}
