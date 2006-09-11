package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;

import de.fuberlin.wiwiss.d2rq.helpers.DummyValueSource;
import junit.framework.TestCase;

public class ValueMakerTest extends TestCase {

	public void testBlankNodeIDToString() {
		BlankNodeID b = new BlankNodeID("table.col1,table.col2", "classmap1");
		assertEquals("BlankNodeID(table.col1,table.col2)", b.toString());
	}

	public void testColumnToString() {
		assertEquals("Column(foo.bar)", new Column("foo.bar").toString());
	}
	
	public void testPatternToString() {
		assertEquals("Pattern(http://test/@@foo.bar@@)", new Pattern("http://test/@@foo.bar@@").toString());
	}
	
	public void testValueDecoratorWithoutTranslatorToString() {
		assertEquals("Column(foo.bar):maxLength=10",
				new ValueDecorator(
						new Column("foo.bar"), 
						Collections.singletonList(ValueDecorator.maxLengthConstraint(10))).toString());
	}
	
	public void testMaxLengthConstraint() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.maxLengthConstraint(5)));
		assertTrue(values.matches(null));
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
		assertTrue(values.matches(null));
		assertTrue(values.matches("foo"));
		assertTrue(values.matches("barfoobaz"));
		assertFalse(values.matches(""));
		assertFalse(values.matches("bar"));
		values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.containsConstraint("")));
		assertTrue(values.matches(null));
		assertTrue(values.matches(""));
		assertTrue(values.matches("a"));
		source.setCouldFit(false);
		assertFalse(values.matches("a"));
	}
	
	public void testRegexConstraint() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueDecorator values = new ValueDecorator(source, Collections.singletonList(ValueDecorator.regexConstraint("^[0-9]{5}$")));
		assertTrue(values.matches(null));
		assertTrue(values.matches("12345"));
		assertFalse(values.matches("abc"));
		source.setCouldFit(false);
		assertFalse(values.matches("12345"));
	}
}
