package de.fuberlin.wiwiss.d2rq.map;

import de.fuberlin.wiwiss.d2rq.helpers.DummyValueSource;
import de.fuberlin.wiwiss.d2rq.map.ContainsRestriction;
import de.fuberlin.wiwiss.d2rq.map.MaxLengthRestriction;
import de.fuberlin.wiwiss.d2rq.map.RegexRestriction;
import junit.framework.TestCase;

/**
 * Unit tests for {@link MaxLengthRestriction}, {@link ContainsRestriction}
 * and {@link RegexRestriction}.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ValueRestrictionTest extends TestCase {

	public void testMaxLengthRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		MaxLengthRestriction maxLength = new MaxLengthRestriction(source, 5);
		assertTrue(maxLength.couldFit(null));
		assertTrue(maxLength.couldFit(""));
		assertTrue(maxLength.couldFit("foo"));
		assertTrue(maxLength.couldFit("fooba"));
		assertFalse(maxLength.couldFit("foobar"));
		source.setCouldFit(false);
		assertFalse(maxLength.couldFit("foo"));
	}
	
	public void testContainsRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		ContainsRestriction contains = new ContainsRestriction(source, "foo");
		assertTrue(contains.couldFit(null));
		assertTrue(contains.couldFit("foo"));
		assertTrue(contains.couldFit("barfoobaz"));
		assertFalse(contains.couldFit(""));
		assertFalse(contains.couldFit("bar"));
		contains = new ContainsRestriction(source, "");
		assertTrue(contains.couldFit(null));
		assertTrue(contains.couldFit(""));
		assertTrue(contains.couldFit("a"));
		source.setCouldFit(false);
		assertFalse(contains.couldFit("a"));
	}
	
	public void testRegexRestriction() {
		DummyValueSource source = new DummyValueSource("foo", true);
		RegexRestriction regex = new RegexRestriction(source, "^[0-9]{5}$");
		assertTrue(regex.couldFit(null));
		assertTrue(regex.couldFit("12345"));
		assertFalse(regex.couldFit("abc"));
		source.setCouldFit(false);
		assertFalse(regex.couldFit("12345"));
	}
}
