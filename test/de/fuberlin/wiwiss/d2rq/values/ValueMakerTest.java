package de.fuberlin.wiwiss.d2rq.values;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.expr.Expression;

public class ValueMakerTest extends TestCase {
	private final static Attribute foo_col1 = new Attribute(null, "foo", "col1");
	private final static Attribute foo_col2 = new Attribute(null, "foo", "col2");
	
	public void testBlankNodeIDToString() {
		BlankNodeID b = new BlankNodeID("classmap1", Arrays.asList(new Attribute[]{foo_col1, foo_col2}));
		assertEquals("BlankNodeID(foo.col1,foo.col2)", b.toString());
	}

	public void testColumnToString() {
		assertEquals("Column(foo.col1)", new Column(foo_col1).toString());
	}
	
	public void testPatternToString() {
		assertEquals("Pattern(http://test/@@foo.bar@@)", new Pattern("http://test/@@foo.bar@@").toString());
	}
	
	public void testValueDecoratorWithoutTranslatorToString() {
		assertEquals("Column(foo.col1):maxLength=10",
				new ValueDecorator(
						new Column(foo_col1), 
						Collections.singletonList(ValueDecorator.maxLengthConstraint(10))).toString());
	}
	
	public void testMaxLengthConstraint() {
		DummyValueMaker source = new DummyValueMaker("foo");
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.maxLengthConstraint(5)));
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
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.containsConstraint("foo")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, "foo"));
		assertTrue(matches(values, "barfoobaz"));
		assertFalse(matches(values, ""));
		assertFalse(matches(values, "bar"));
		values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.containsConstraint("")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, ""));
		assertTrue(matches(values, "a"));
		source.setSelectCondition(Expression.FALSE);
		assertFalse(matches(values, "a"));
	}
	
	public void testRegexConstraint() {
		DummyValueMaker source = new DummyValueMaker("foo");
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.regexConstraint("^[0-9]{5}$")));
		assertFalse(matches(values, null));
		assertTrue(matches(values, "12345"));
		assertFalse(matches(values, "abc"));
		source.setSelectCondition(Expression.FALSE);
		assertFalse(matches(values, "12345"));
	}
	
	public void testColumnDoesNotMatchNull() {
		Column column = new Column(foo_col1);
		assertFalse(matches(column, null));
	}
	
	public void testPatternDoesNotMatchNull() {
		Pattern pattern = new Pattern("foo/@@foo.bar@@");
		assertFalse(matches(pattern, null));
	}
	
	public void testBlankNodeIDDoesNotMatchNull() {
		BlankNodeID bNodeID = new BlankNodeID("classmap", Collections.singletonList(foo_col1));
		assertFalse(matches(bNodeID, null));
	}
	
	private boolean matches(ValueMaker valueMaker, String value) {
		return !valueMaker.valueExpression(value).isFalse();
	}
}
