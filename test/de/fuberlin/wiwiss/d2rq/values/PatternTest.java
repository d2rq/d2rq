package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Constant;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.ResultRowMap;
import de.fuberlin.wiwiss.d2rq.sql.SQL;

/**
 * Tests the {@link Pattern} class.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
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
		assertTrue(matches(p, "http://www.example.org/dbserver01/db01#Paper1111-2222222-333.rdf"));
	}

	public void testMatchesTrivialPattern() {
		Pattern p = new Pattern("foobar");
		assertPatternValues(p, "foobar", new HashMap<String,String>());
		assertFalse(matches(p, "fooba"));
		assertFalse(matches(p, "foobarb"));
		assertFalse(matches(p, "oobar"));
		assertFalse(matches(p, "ffoobar"));
		assertFalse(matches(p, null));
	}

	public void testMatchesMiniPattern() {
		Pattern p = new Pattern("@@table.col1@@");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "");
		assertPatternValues(p, "", map);
		map.put("table.col1", "a");
		assertPatternValues(p, "a", map);
		map.put("table.col1", "xyz");
		assertPatternValues(p, "xyz", map);
		assertFalse(matches(p, null));
	}

	/**
	 * We use regular expressions to match patterns and they behave
	 * oddly around newlines
	 */
	public void testMatchesPatternContainingNewlines() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "1\n2");
		assertPatternValues(p, "foo1\n2bar", map);
	}

	/**
	 * We use regular expressions to match patterns; make sure the
	 * implementation correctly escapes magic characters in the pattern
	 */
	public void testMagicRegexCharactersCauseNoProblems() {
		Pattern p = new Pattern("(foo|bar)@@table.col1@@");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "1");
		assertPatternValues(p, "(foo|bar)1", map);
		assertFalse(matches(p, "foo1"));
	}

	public void testMatchesOneColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@bar");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "1");
		assertPatternValues(p, "foo1bar", map);
		map.put("table.col1", "");
		assertPatternValues(p, "foobar", map);
		map.put("table.col1", "foofoobarbar");
		assertPatternValues(p, "foofoofoobarbarbar", map);
		assertFalse(matches(p, "fooba"));
		assertFalse(matches(p, "barfoo"));
		assertFalse(matches(p, "fobar"));
	}

	public void testMatchesTwoColumnPattern() {
		Pattern p = new Pattern("foo@@table.col1@@-@@table.col2@@baz");
		Map<String,String> map = new HashMap<String,String>();
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
		assertFalse(matches(p, "foo1-"));
		assertFalse(matches(p, "foobaz-"));
		assertFalse(matches(p, "foo1-2baz3"));
	}

	public void testMatchesPatternStartingWithColumn() {
		Pattern p = new Pattern("@@table.col1@@bar@@table.col2@@baz");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "");
		map.put("table.col2", "");
		assertPatternValues(p, "barbaz", map);
		map.put("table.col1", "1");
		map.put("table.col2", "2");
		assertPatternValues(p, "1bar2baz", map);
		map.put("table.col1", "baz");
		map.put("table.col2", "foo");
		assertPatternValues(p, "bazbarfoobaz", map);
		assertFalse(matches(p, "1bar"));
		assertFalse(matches(p, "bazbar"));
		assertFalse(matches(p, "1bar2baz3"));
	}

	public void testMatchesPatternEndingWithColumn() {
		Pattern p = new Pattern("foo@@table.col1@@bar@@table.col2@@");
		Map<String,String> map = new HashMap<String,String>();
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
		Iterator<Object> it = new Pattern("foo").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorFirstLiteralThenColumn() {
		Iterator<Object> it = new Pattern("foo@@table.col1@@").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertTrue(it.hasNext());
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorFirstColumnThenLiteral() {
		Iterator<Object> it = new Pattern("@@table.col1@@foo").partsIterator();
		assertTrue(it.hasNext());
		assertEquals("", it.next());
		assertTrue(it.hasNext());
		assertEquals(col1, it.next());
		assertTrue(it.hasNext());
		assertEquals("foo", it.next());
		assertFalse(it.hasNext());
	}
	
	public void testPartsIteratorSeveralColumns() {
		Iterator<Object> it = new Pattern("foo@@table.col1@@bar@@table.col2@@").partsIterator();
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
		Iterator<Object> it = new Pattern("@@table.col1@@@@table.col2@@").partsIterator();
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
	
	public void testSamePatternsAreEqual() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("foo@@table.col1@@");
		assertEquals(p1, p2);
		assertEquals(p2, p1);
		assertEquals(p1.hashCode(), p2.hashCode());
	}
	
	public void testPatternsWithDifferentColumnsAreNotEqual() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("foo@@table.col2@@");
		assertFalse(p1.equals(p2));
		assertFalse(p2.equals(p1));
		assertFalse(p1.hashCode() == p2.hashCode());
	}

	public void testPatternsWithDifferentLiteralPartsAreNotEqual() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("bar@@table.col1@@");
		assertFalse(p1.equals(p2));
		assertFalse(p2.equals(p1));
		assertFalse(p1.hashCode() == p2.hashCode());
	}

	public void testIdenticalPatternsAreCompatible() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("foo@@table.col1@@");
		assertTrue(p1.isEquivalentTo(p2));
		assertTrue(p2.isEquivalentTo(p1));
	}
	
	public void testPatternsWithDifferentColumnNamesAreCompatible() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("foo@@table.col2@@");
		assertTrue(p1.isEquivalentTo(p2));
		assertTrue(p2.isEquivalentTo(p1));
	}
	
	public void testPatternsWithDifferentLiteralPartsAreNotCompatible() {
		Pattern p1 = new Pattern("foo@@table.col1@@");
		Pattern p2 = new Pattern("bar@@table.col1@@");
		assertFalse(p1.isEquivalentTo(p2));
		assertFalse(p2.isEquivalentTo(p1));
	}
	
	public void testMultiColumnPatternsWithDifferentLiteralPartsAreNotCompatible() {
		Pattern p1 = new Pattern("foo@@table.col1@@bar@@table.col2@@abc");
		Pattern p2 = new Pattern("foo@@table.col1@@bar@@table.col2@@xyz");
		assertFalse(p1.isEquivalentTo(p2));
		assertFalse(p2.isEquivalentTo(p1));
	}
	
	public void testLiteralPatternsMatchTrivialRegex() {
		assertTrue(new Pattern("asdf").literalPartsMatchRegex(".*"));
	}
	
	public void testLiteralPatternsDontMatchTrivialRegex() {
		assertFalse(new Pattern("asdf").literalPartsMatchRegex("foo"));
	}
	
	public void testLiteralPatternRegexIsAnchored() {
		assertFalse(new Pattern("aaa").literalPartsMatchRegex("b*"));
	}
	
	public void testLiteralPatternRegexMultipleParts() {
		assertTrue(new Pattern("aaa@@aaa.aaa@@aaa").literalPartsMatchRegex("aaa"));
	}
	
	public void testLiteralPatternRegexMatchesOnlyLiteralParts() {
		assertTrue(new Pattern("aaa@@bbb.ccc@@aaa").literalPartsMatchRegex("a+"));
	}

	public void testPatternURLEncode() {
		Pattern p = new Pattern("aaa@@table.col1|urlencode@@bbb");
		assertPattern("aaax+ybbb", p.makeValue(row("x y")));
		assertPatternValues(p, "aaax+ybbb", Collections.singletonMap("table.col1", "x y"));
	}
	
	public void testPatternEncode() {
		Pattern p = new Pattern("aaa@@table.col1|encode@@bbb");
		assertPattern("aaahello%20world%21bbb", p.makeValue(row("hello world!")));
		
		assertPattern("aaa%3A%3B%3C%3D%3E%3F%40bbb", p.makeValue(row(":;<=>?@")));
		assertPattern("aaa%5B%5C%5D%5E%60bbb", p.makeValue(row("[\\]^`")));
		
		assertPatternValues(p, "aaa%7B%7C%7Dbbb", Collections.singletonMap("table.col1", "{|}"));
	}
	
	public void testPatternURLEncodeIllegal() {
		Pattern p = new Pattern("@@table.col1|urlencode@@");
		assertFalse(matches(p, "%"));
	}
	
	public void testPatternURLify() {
		Pattern p = new Pattern("aaa@@table.col1|urlify@@bbb");
		assertPattern("aaax_ybbb", p.makeValue(row("x y")));
		assertPatternValues(p, "aaax_ybbb", Collections.singletonMap("table.col1", "x y"));
	}
	
	public void testPatternURLifyEscapeUnderscore() {
		Pattern p = new Pattern("aaa@@table.col1|urlify@@bbb");
		assertPattern("aaax%5Fybbb", p.makeValue(row("x_y")));
		assertPatternValues(p, "aaax%5Fybbb", Collections.singletonMap("table.col1", "x_y"));
	}
	
	public void testTrivialPatternFirstPart() {
		assertEquals("aaa", new Pattern("aaa").firstLiteralPart());
	}
	
	public void testTrivialPatternLastPart() {
		assertEquals("aaa", new Pattern("aaa").lastLiteralPart());
	}

	public void testEmptyFirstPart() {
		assertEquals("", new Pattern("@@table.col1@@aaa").firstLiteralPart());
	}
	
	public void testEmptyLastPart() {
		assertEquals("", new Pattern("aaa@@table.col1@@").lastLiteralPart());
	}

	public void testFirstAndLastPart() {
		assertEquals("aaa", new Pattern("aaa@@table.col1@@bbb").firstLiteralPart());
		assertEquals("bbb", new Pattern("aaa@@table.col1@@bbb").lastLiteralPart());
	}
	
	private void assertPattern(String expected, String pattern) {
		Pattern p = new Pattern(pattern);
		assertEquals(expected, p.makeValue(this.row));
	}
	
	private void assertPatternValues(Pattern pattern, String value, Map<String,String> expectedValues) {
		assertTrue(matches(pattern, value));
		Collection<Expression> expressions = new HashSet<Expression>();
		for (String attributeName: expectedValues.keySet()) {
			String attributeValue = (String) expectedValues.get(attributeName);
			Attribute attribute = SQL.parseAttribute(attributeName);
			expressions.add(Equality.create(
					new AttributeExpr(attribute), 
					new Constant(attributeValue, attribute)));
		}
		Expression expr = Conjunction.create(expressions);
		assertEquals(expr, pattern.valueExpression(value));
	}

	private boolean matches(ValueMaker valueMaker, String value) {
		return !valueMaker.valueExpression(value).isFalse();
	}

	private ResultRow row(String spec) {
		String[] parts = spec.split("\\|", -1);
		Attribute[] columns = {col1, col2, col3, col4, col5};
		Map<ProjectionSpec,String> result = new HashMap<ProjectionSpec,String>();
		for (int i = 0; i < parts.length && i < columns.length; i++) {
			result.put(columns[i], parts[i]);
		}
		return new ResultRowMap(result);
	}
}
