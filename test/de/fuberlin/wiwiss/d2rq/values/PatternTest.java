package de.fuberlin.wiwiss.d2rq.values;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.ResultRowMap;
import de.fuberlin.wiwiss.d2rq.sql.SQL;

/**
 * Tests the {@link Pattern} class.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: PatternTest.java,v 1.3 2006/09/15 15:31:22 cyganiak Exp $
 */
public class PatternTest extends TestCase {
	private final static Attribute col1 = new Attribute(null, "table", "col1");
	private final static Attribute col2 = new Attribute(null, "table", "col2");
	private final static Attribute col3 = new Attribute(null, "table", "col3");
	private final static Attribute col4 = new Attribute(null, "table", "col4");
	private final static Attribute col5 = new Attribute(null, "table", "col5");

	private ResultRow row;

	public void setUp() {
		this.row = row("1|2|3|4|");
	}

	public void testSimple() {
		Pattern pattern = new Pattern("foo@@table.col1@@baz");
		assertEquals("foo1baz", pattern.makeValue(row("1")));
	}
	
	public void testNull() {
		Pattern pattern = new Pattern("foo@@table.col1@@bar@@table.col2@@baz");
		assertNull(pattern.makeValue(row("123")));
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
		assertTrue(p.matches("http://www.example.org/dbserver01/db01#Paper1111-2222222-333.rdf"));
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
		Map map = new HashMap();
		map.put("table.col1", "");
		assertPatternValues(p, "", map);
		map.put("table.col1", "a");
		assertPatternValues(p, "a", map);
		map.put("table.col1", "xyz");
		assertPatternValues(p, "xyz", map);
		assertNoMatch(p, null);
	}

	/**
	 * We use regular expressions to match patterns and they behave
	 * oddly around newlines
	 */
	public void testMatchesPatternContainingNewlines() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		Map map = new HashMap();
		map.put("table.col1", "1\n2");
		assertPatternValues(p, "foo1\n2bar", map);
	}

	/**
	 * We use regular expressions to match patterns; make sure the
	 * implementation correctly escapes magic characters in the pattern
	 */
	public void testMagicRegexCharactersCauseNoProblems() {
		Pattern p = new Pattern("(foo|bar)@@table.col1@@");
		Map map = new HashMap();
		map.put("table.col1", "1");
		assertPatternValues(p, "(foo|bar)1", map);
		assertNoMatch(p, "foo1");
	}

	public void testMatchesOneColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		Map map = new HashMap();
		map.put("table.col1", "1");
		assertPatternValues(p, "foo1bar", map);
		map.put("table.col1", "");
		assertPatternValues(p, "foobar", map);
		map.put("table.col1", "foofoobarbar");
		assertPatternValues(p, "foofoofoobarbarbar", map);
		assertNoMatch(p, "fooba");
		assertNoMatch(p, "barfoo");
		assertNoMatch(p, "fobar");
	}

	public void testMatchesTwoColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@-@@table.col2@@baz");
		Map map = new HashMap();
		map.put("table.col1", "");
		map.put("table.col2", "");
		assertPatternValues(p, "foo-baz", map);
		map.put("table.col1", "1");
		map.put("table.col2", "2");
		assertPatternValues(p, "foo1-2baz", map);
		map.put("table.col1", "baz");
		map.put("table.col2", "foo");
		assertPatternValues(p, "foobaz-foobaz", map);
		map.put("table.col1", "XYZ");
		map.put("table.col2", "XYZ-2");
		assertPatternValues(p, "fooXYZ-XYZ-2baz", map);
		assertNoMatch(p, "foo1-");
		assertNoMatch(p, "foobaz-");
		assertNoMatch(p, "foo1-2baz3");
	}

	public void testMatchesPatternStartingWithColumn() {
		Pattern p = new Pattern("@@table.col1@@bar@@table.col2@@baz");
		Map map = new HashMap();
		map.put("table.col1", "");
		map.put("table.col2", "");
		assertPatternValues(p, "barbaz", map);
		map.put("table.col1", "1");
		map.put("table.col2", "2");
		assertPatternValues(p, "1bar2baz", map);
		map.put("table.col1", "baz");
		map.put("table.col2", "foo");
		assertPatternValues(p, "bazbarfoobaz", map);
		assertNoMatch(p, "1bar");
		assertNoMatch(p, "bazbar");
		assertNoMatch(p, "1bar2baz3");
	}

	public void testMatchesPatternEndingWithColumn() {
		Pattern p = new Pattern("foo@@table.col1@@bar@@table.col2@@");
		Map map = new HashMap();
		map.put("table.col1", "");
		map.put("table.col2", "");
		assertPatternValues(p, "foobar", map);
		map.put("table.col1", "1");
		map.put("table.col2", "2");
		assertPatternValues(p, "foo1bar2", map);
		map.put("table.col1", "baz");
		map.put("table.col2", "foo");
		assertPatternValues(p, "foobazbarfoo", map);
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
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorFirstColumnThenLiteral() {
		Iterator it = new Pattern("@@table.col1@@foo").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorSeveralColumns() {
		Iterator it = new Pattern("foo@@table.col1@@bar@@table.col2@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertTrue(it.hasNext());
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("bar", it.next());
		assertTrue(it.hasNext());
		assertEquals(col2, it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorAdjacentColumns() {
		Iterator it = new Pattern("@@table.col1@@@@table.col2@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(col2, it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testToString() {
		assertEquals("Pattern(foo@@table.col1@@)", new Pattern("foo@@table.col1@@").toString());
	}
	
	private void assertPattern(String expected, String pattern) {
		Pattern p = new Pattern(pattern);
		assertEquals(expected, p.makeValue(this.row));
	}
	
	private void assertPatternValues(Pattern pattern, String value, Map expectedValues) {
		assertTrue(pattern.matches(value));
		Map actualValues = pattern.attributeConditions(value);
		Iterator it = expectedValues.keySet().iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Attribute column = SQL.parseAttribute(name);
			assertTrue("missing column " + column, actualValues.containsKey(column));
			assertEquals(expectedValues.get(name), actualValues.get(column));
		}
		it = actualValues.keySet().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			assertTrue("unexpected column " + column, expectedValues.containsKey(column.qualifiedName()));
			assertEquals(expectedValues.get(column.qualifiedName()), actualValues.get(column));
		}
	}
	
	private void assertNoMatch(Pattern pattern, String value) {
		assertFalse(pattern.matches(value));
		assertTrue(pattern.attributeConditions(value).isEmpty());
	}
	
	private ResultRow row(String spec) {
		String[] parts = spec.split("\\|", -1);
		Attribute[] columns = {col1, col2, col3, col4, col5};
		Map result = new HashMap();
		for (int i = 0; i < parts.length && i < columns.length; i++) {
			result.put(columns[i], parts[i]);
		}
		return new ResultRowMap(result);
	}
}
