package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.helpers.DummyValueSource;

/**
 * Unit tests for {@link MaxLengthRestriction}, {@link ContainsRestriction}
 * and {@link RegexRestriction}.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ValueRestrictionTest extends TestCase {

	public void testMaxLengthRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueMaker values = new ValueMaker(source, Collections.singletonList(ValueMaker.maxLengthConstraint(5)));
		assertTrue(values.matches(null));
		assertTrue(values.matches(""));
		assertTrue(values.matches("foo"));
		assertTrue(values.matches("fooba"));
		assertFalse(values.matches("foobar"));
		source.setCouldFit(false);
		assertFalse(values.matches("foo"));
	}
	
	public void testContainsRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueMaker values = new ValueMaker(source, Collections.singletonList(ValueMaker.containsConstraint("foo")));
		assertTrue(values.matches(null));
		assertTrue(values.matches("foo"));
		assertTrue(values.matches("barfoobaz"));
		assertFalse(values.matches(""));
		assertFalse(values.matches("bar"));
		values = new ValueMaker(source, Collections.singletonList(ValueMaker.containsConstraint("")));
		assertTrue(values.matches(null));
		assertTrue(values.matches(""));
		assertTrue(values.matches("a"));
		source.setCouldFit(false);
		assertFalse(values.matches("a"));
	}
	
	public void testRegexRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ValueMaker values = new ValueMaker(source, Collections.singletonList(ValueMaker.regexConstraint("^[0-9]{5}$")));
		assertTrue(values.matches(null));
		assertTrue(values.matches("12345"));
		assertFalse(values.matches("abc"));
		source.setCouldFit(false);
		assertFalse(values.matches("12345"));
	}
}
