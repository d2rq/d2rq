package de.fuberlin.wiwiss.d2rq.values;

import java.util.Arrays;
import java.util.Collections;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;

import junit.framework.TestCase;

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
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.maxLengthConstraint(5)));
		assertFalse(values.matches(null));
		assertTrue(values.matches(""));
		assertTrue(values.matches("foo"));
		assertTrue(values.matches("fooba"));
		assertFalse(values.matches("foobar"));
		source.setCouldFit(false);
		assertFalse(values.matches("foo"));
	}
	
	public void testContainsConstraint() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.containsConstraint("foo")));
		assertFalse(values.matches(null));
		assertTrue(values.matches("foo"));
		assertTrue(values.matches("barfoobaz"));
		assertFalse(values.matches(""));
		assertFalse(values.matches("bar"));
		values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.containsConstraint("")));
		assertFalse(values.matches(null));
		assertTrue(values.matches(""));
		assertTrue(values.matches("a"));
		source.setCouldFit(false);
		assertFalse(values.matches("a"));
	}
	
	public void testRegexConstraint() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.regexConstraint("^[0-9]{5}$")));
		assertFalse(values.matches(null));
		assertTrue(values.matches("12345"));
		assertFalse(values.matches("abc"));
		source.setCouldFit(false);
		assertFalse(values.matches("12345"));
	}
	
	public void testColumnDoesNotMatchNull() {
		Column column = new Column(foo_col1);
		assertFalse(column.matches(null));
	}
	
	public void testPatternDoesNotMatchNull() {
		Pattern pattern = new Pattern("foo/@@foo.bar@@");
		assertFalse(pattern.matches(null));
	}
	
	public void testBlankNodeIDDoesNotMatchNull() {
		BlankNodeID bNodeID = new BlankNodeID("classmap", Collections.singletonList(foo_col1));
		assertFalse(bNodeID.matches(null));
	}
}
