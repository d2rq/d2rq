package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Pattern;

/**
 * Tests the {@link Pattern} class.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: PatternTest.java,v 1.1 2006/09/03 12:57:30 cyganiak Exp $
 */
public class PatternTest extends TestCase {
	private Map map;
	private final static String[] defaultRow = {"1", "2", "3", "4", ""};

	public void setUp() {
		this.map = new HashMap();
		this.map.put("table.col1", new Integer(0));
		this.map.put("table.col2", new Integer(1));
		this.map.put("table.col3", new Integer(2));
		this.map.put("table.col4", new Integer(3));
		this.map.put("table.col5", new Integer(4));
	}

	public void testSimple() {
		Pattern pattern = new Pattern("foo@@table.col1@@baz");
		String[] row = {"1"};
		assertEquals("foo1baz", pattern.getValue(row, this.map));
	}
	
	public void testNull() {
		Pattern pattern = new Pattern("foo@@table.col1@@bar@@table.col2@@baz");
		String[] row = {"123", null};
		assertNull(pattern.getValue(row, this.map));
	}
	
	public void testPatternSyntax() {
		assertPattern("foo", "foo");
		assertPattern("1", "@@table.col1@@");
		assertPattern("foo1", "foo@@table.col1@@");
		assertPattern("01", "0@@table.col1@@");
		assertPattern("1foo", "@@table.col1@@foo");
		assertPattern("12", "@@table.col1@@2");
		assertPattern("", "@@table.col5@@");
		assertPattern("foobar", "foo@@table.col5@@bar");
		assertPattern("foo1bar2baz", "foo@@table.col1@@bar@@table.col2@@baz");
		assertPattern("foo2bar1baz", "foo@@table.col2@@bar@@table.col1@@baz");
		assertPattern("foo@bar", "foo@bar");
		assertPattern("1@", "@@table.col1@@@");
		assertPattern("12", "@@table.col1@@@@table.col2@@");
		assertPattern("@1@", "@@@table.col1@@@");
		// These patterns were previously considered broken, now we allow them
		assertPattern("@@", "@@");
		assertPattern("@@@@", "@@@@");
		assertPattern("foo@@bar", "foo@@bar");
		assertPattern("foo1bar@@", "foo@@table.col1@@bar@@");
		assertPattern("foo1bar@@baz", "foo@@table.col1@@bar@@baz");
		assertPattern("1@@", "@@table.col1@@@@");
		assertPattern("@@1", "@@@@table.col1@@");
		assertPattern("@table.col1@@", "@table.col1@@");
		assertPattern("1@@@@@", "@@table.col1@@@@@@@");
	}

	public void testMatches() {
		Pattern p = new Pattern("http://www.example.org/dbserver01/db01#Paper@@Papers.PaperID@@-@@Persons.PersonID@@-@@Conferences.ConfID@@.rdf");
		assertTrue(p.couldFit("http://www.example.org/dbserver01/db01#Paper1111-2222222-333.rdf"));
	}

	public void testMatchesTrivialPattern() {
		Pattern p = new Pattern("foobar");
		assertPatternValues(p, "foobar", new HashMap());
		assertNoMatch(p, "fooba");
		assertNoMatch(p, "foobarb");
		assertNoMatch(p, "oobar");
		assertNoMatch(p, "ffoobar");
		assertNoMatch(p, null);
	}

	public void testMatchesMiniPattern() {
		Pattern p = new Pattern("@@table.col1@@");
		this.map = new HashMap();
		this.map.put("table.col1", "");
		assertPatternValues(p, "", this.map);
		this.map.put("table.col1", "a");
		assertPatternValues(p, "a", this.map);
		this.map.put("table.col1", "xyz");
		assertPatternValues(p, "xyz", this.map);
		assertNoMatch(p, null);
	}

	/**
	 * We use regular expressions to match patterns and they behave
	 * oddly around newlines
	 */
	public void testMatchesPatternContainingNewlines() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		this.map = new HashMap();
		this.map.put("table.col1", "1\n2");
		assertPatternValues(p, "foo1\n2bar", this.map);
	}

	/**
	 * We use regular expressions to match patterns; make sure the
	 * implementation correctly escapes magic characters in the pattern
	 */
	public void testMagicRegexCharactersCauseNoProblems() {
		Pattern p = new Pattern("(foo|bar)@@table.col1@@");
		this.map = new HashMap();
		this.map.put("table.col1", "1");
		assertPatternValues(p, "(foo|bar)1", this.map);
		assertNoMatch(p, "foo1");
	}

	public void testMatchesOneColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		this.map = new HashMap();
		this.map.put("table.col1", "1");
		assertPatternValues(p, "foo1bar", this.map);
		this.map.put("table.col1", "");
		assertPatternValues(p, "foobar", this.map);
		this.map.put("table.col1", "foofoobarbar");
		assertPatternValues(p, "foofoofoobarbarbar", this.map);
		assertNoMatch(p, "fooba");
		assertNoMatch(p, "barfoo");
		assertNoMatch(p, "fobar");
	}

	public void testMatchesTwoColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@-@@table.col2@@baz");
		this.map = new HashMap();
		this.map.put("table.col1", "");
		this.map.put("table.col2", "");
		assertPatternValues(p, "foo-baz", this.map);
		this.map.put("table.col1", "1");
		this.map.put("table.col2", "2");
		assertPatternValues(p, "foo1-2baz", this.map);
		this.map.put("table.col1", "baz");
		this.map.put("table.col2", "foo");
		assertPatternValues(p, "foobaz-foobaz", this.map);
		this.map.put("table.col1", "XYZ");
		this.map.put("table.col2", "XYZ-2");
		assertPatternValues(p, "fooXYZ-XYZ-2baz", this.map);
		assertNoMatch(p, "foo1-");
		assertNoMatch(p, "foobaz-");
		assertNoMatch(p, "foo1-2baz3");
	}

	public void testMatchesPatternStartingWithColumn() {
		Pattern p = new Pattern("@@table.col1@@bar@@table.col2@@baz");
		this.map = new HashMap();
		this.map.put("table.col1", "");
		this.map.put("table.col2", "");
		assertPatternValues(p, "barbaz", this.map);
		this.map.put("table.col1", "1");
		this.map.put("table.col2", "2");
		assertPatternValues(p, "1bar2baz", this.map);
		this.map.put("table.col1", "baz");
		this.map.put("table.col2", "foo");
		assertPatternValues(p, "bazbarfoobaz", this.map);
		assertNoMatch(p, "1bar");
		assertNoMatch(p, "bazbar");
		assertNoMatch(p, "1bar2baz3");
	}

	public void testMatchesPatternEndingWithColumn() {
		Pattern p = new Pattern("foo@@table.col1@@bar@@table.col2@@");
		this.map = new HashMap();
		this.map.put("table.col1", "");
		this.map.put("table.col2", "");
		assertPatternValues(p, "foobar", this.map);
		this.map.put("table.col1", "1");
		this.map.put("table.col2", "2");
		assertPatternValues(p, "foo1bar2", this.map);
		this.map.put("table.col1", "baz");
		this.map.put("table.col2", "foo");
		assertPatternValues(p, "foobazbarfoo", this.map);
	}

	public void testPartsIteratorSingleLiteral() {
		Iterator it = new Pattern("foo").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorFirstLiteralThenColumn() {
		Iterator it = new Pattern("foo@@table.col1@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col1"), it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorFirstColumnThenLiteral() {
		Iterator it = new Pattern("@@table.col1@@foo").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col1"), it.next());
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorSeveralColumns() {
		Iterator it = new Pattern("foo@@table.col1@@bar@@table.col2@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col1"), it.next());
		assertTrue(it.hasNext());
		assertEquals("bar", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col2"), it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorAdjacentColumns() {
		Iterator it = new Pattern("@@table.col1@@@@table.col2@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col1"), it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(new Column("table.col2"), it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testToString() {
		assertEquals("Pattern(foo@@table.col1@@)", new Pattern("foo@@table.col1@@").toString());
	}
	
	private void assertPattern(String expected, String pattern) {
		Pattern p = new Pattern(pattern);
		assertEquals(expected, p.getValue(defaultRow, this.map));
	}
	
	private void assertPatternValues(Pattern pattern, String value, Map expectedValues) {
		assertTrue(pattern.couldFit(value));
		Map actualValues = pattern.getColumnValues(value);
		Iterator it = expectedValues.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Column column = new Column(name);
			assertTrue("missing column " + column, actualValues.containsKey(column));
			assertEquals(expectedValues.get(name), actualValues.get(column));
		}
		it = actualValues.keySet().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			assertTrue("unexpected column " + column, expectedValues.containsKey(column.getQualifiedName()));
			assertEquals(expectedValues.get(column.getQualifiedName()), actualValues.get(column));
		}
	}
	
	private void assertNoMatch(Pattern pattern, String value) {
		assertFalse(pattern.couldFit(value));
		assertTrue(pattern.getColumnValues(value).isEmpty());
	}
}
