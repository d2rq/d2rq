/*
 * $Id: ValueRestrictionTest.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import junit.framework.TestCase;

/**
 * Unit tests for {@link MaxLengthRestriction}, {@link ContainsRestriction}
 * and {@link RegexRestriction}.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class ValueRestrictionTest extends TestCase {

	/**
	 * Constructor for ValueRestrictionTest.
	 * @param arg0
	 */
	public ValueRestrictionTest(String arg0) {
		super(arg0);
	}

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
