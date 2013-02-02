package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.d2rq.db.DummyDB;
import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.values.TemplateValueMaker;
import org.junit.Before;
import org.junit.Test;


/**
 * TOOD: This mixes tests for {@link Microsyntax#parsePattern(String)} and {@link TemplateValueMaker}; separate these
 */
public class PatternTest {
	private ColumnName col1, col2, col3, col4, col5;
	private TableOp table;
	private ResultRow row;

	@Before
	public void setUp() {
		col1 = Microsyntax.parseColumn("table.col1");
		col2 = Microsyntax.parseColumn("table.col2");
		col3 = Microsyntax.parseColumn("table.col3");
		col4 = Microsyntax.parseColumn("table.col4");
		col5 = Microsyntax.parseColumn("table.col5");
		table = DummyDB.createTable("table", "col1", "col2", "col3", "col4", "col5");
		row = row("1|2|3|4|");
	}

	@Test
	public void testSimple() {
		TemplateValueMaker pattern = create("foo@@table.col1@@baz");
		assertEquals("foo1baz", pattern.makeValue(row("1")));
	}
	
	@Test
	public void testNull() {
		TemplateValueMaker pattern = create("foo@@table.col1@@bar@@table.col2@@baz");
		assertNull(pattern.makeValue(row("123")));
	}
	
	@Test
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

	@Test
	public void testMatches() {
		TemplateValueMaker p = create("http://www.example.org/dbserver01/db01#Paper@@Papers.PaperID@@-@@Persons.PersonID@@-@@Conferences.ConfID@@.rdf");
		assertTrue(matches(p, "http://www.example.org/dbserver01/db01#Paper1111-2222222-333.rdf"));
	}

	@Test
	public void testMatchesTrivialPattern() {
		TemplateValueMaker p = create("foobar");
		assertPatternValues(p, "foobar", new HashMap<String,String>());
		assertFalse(matches(p, "fooba"));
		assertFalse(matches(p, "foobarb"));
		assertFalse(matches(p, "oobar"));
		assertFalse(matches(p, "ffoobar"));
		assertFalse(matches(p, null));
	}

	@Test
	public void testMatchesMiniPattern() {
		TemplateValueMaker p = create("@@table.col1@@");
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
	@Test
	public void testMatchesPatternContainingNewlines() {
		TemplateValueMaker p = create("foo@@table.col1@@bar");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "1\n2");
		assertPatternValues(p, "foo1\n2bar", map);
	}

	/**
	 * We use regular expressions to match patterns; make sure the
	 * implementation correctly escapes magic characters in the pattern
	 */
	@Test
	public void testMagicRegexCharactersCauseNoProblems() {
		TemplateValueMaker p = create("(foo|bar)@@table.col1@@");
		Map<String,String> map = new HashMap<String,String>();
		map.put("table.col1", "1");
		assertPatternValues(p, "(foo|bar)1", map);
		assertFalse(matches(p, "foo1"));
	}

	@Test
	public void testMatchesOneColumnPattern() {
		TemplateValueMaker p = create("foo@@table.col1@@bar");
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

	@Test
	public void testMatchesTwoColumnPattern() {
		TemplateValueMaker p = create("foo@@table.col1@@-@@table.col2@@baz");
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

	@Test
	public void testMatchesPatternStartingWithColumn() {
		TemplateValueMaker p = create("@@table.col1@@bar@@table.col2@@baz");
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

	@Test
	public void testMatchesPatternEndingWithColumn() {
		TemplateValueMaker p = create("foo@@table.col1@@bar@@table.col2@@");
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

	@Test
	public void testToString() {
		assertEquals("foo@@table.col1@@", Microsyntax.toString(create("foo@@table.col1@@")));
	}
	
	@Test
	public void testSamePatternsAreEqual() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("foo@@table.col1@@");
		assertEquals(p1, p2);
		assertEquals(p2, p1);
		assertEquals(p1.hashCode(), p2.hashCode());
	}
	
	@Test
	public void testPatternsWithDifferentColumnsAreNotEqual() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("foo@@table.col2@@");
		assertFalse(p1.equals(p2));
		assertFalse(p2.equals(p1));
		assertFalse(p1.hashCode() == p2.hashCode());
	}

	@Test
	public void testPatternsWithDifferentLiteralPartsAreNotEqual() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("bar@@table.col1@@");
		assertFalse(p1.equals(p2));
		assertFalse(p2.equals(p1));
		assertFalse(p1.hashCode() == p2.hashCode());
	}

	@Test
	public void testIdenticalPatternsAreCompatible() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("foo@@table.col1@@");
		assertTrue(p1.isEquivalentTo(p2));
		assertTrue(p2.isEquivalentTo(p1));
	}
	
	@Test
	public void testPatternsWithDifferentColumnNamesAreCompatible() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("foo@@table.col2@@");
		assertTrue(p1.isEquivalentTo(p2));
		assertTrue(p2.isEquivalentTo(p1));
	}
	
	@Test
	public void testPatternsWithDifferentLiteralPartsAreNotCompatible() {
		TemplateValueMaker p1 = create("foo@@table.col1@@");
		TemplateValueMaker p2 = create("bar@@table.col1@@");
		assertFalse(p1.isEquivalentTo(p2));
		assertFalse(p2.isEquivalentTo(p1));
	}
	
	@Test
	public void testMultiColumnPatternsWithDifferentLiteralPartsAreNotCompatible() {
		TemplateValueMaker p1 = create("foo@@table.col1@@bar@@table.col2@@abc");
		TemplateValueMaker p2 = create("foo@@table.col1@@bar@@table.col2@@xyz");
		assertFalse(p1.isEquivalentTo(p2));
		assertFalse(p2.isEquivalentTo(p1));
	}
	
	@Test
	public void testPatternURLEncode() {
		TemplateValueMaker p = create("aaa@@table.col1|urlencode@@bbb");
		assertPattern("aaax+ybbb", p.makeValue(row("x y")));
		assertPatternValues(p, "aaax+ybbb", Collections.singletonMap("table.col1", "x y"));
	}
	
	@Test
	public void testPatternEncode() {
		TemplateValueMaker p = create("aaa@@table.col1|encode@@bbb");
		assertPattern("aaahello%20world%21bbb", p.makeValue(row("hello world!")));
		
		assertPattern("aaa%3A%3B%3C%3D%3E%3F%40bbb", p.makeValue(row(":;<=>?@")));
		assertPattern("aaa%5B%5C%5D%5E%60bbb", p.makeValue(row("[\\]^`")));
		
		assertPatternValues(p, "aaa%7B%7C%7Dbbb", Collections.singletonMap("table.col1", "{|}"));
	}
	
	@Test
	public void testPatternURLEncodeIllegal() {
		TemplateValueMaker p = create("@@table.col1|urlencode@@");
		assertFalse(matches(p, "%"));
	}
	
	@Test
	public void testPatternURLify() {
		TemplateValueMaker p = create("aaa@@table.col1|urlify@@bbb");
		assertPattern("aaax_ybbb", p.makeValue(row("x y")));
		assertPatternValues(p, "aaax_ybbb", Collections.singletonMap("table.col1", "x y"));
	}
	
	public void testPatternURLifyEscapeUnderscore() {
		TemplateValueMaker p = create("aaa@@table.col1|urlify@@bbb");
		assertPattern("aaax%5Fybbb", p.makeValue(row("x_y")));
		assertPatternValues(p, "aaax%5Fybbb", Collections.singletonMap("table.col1", "x_y"));
	}
	
	@Test
	public void testTrivialPatternFirstPart() {
		assertEquals("aaa", create("aaa").firstLiteralPart());
	}
	
	@Test
	public void testTrivialPatternLastPart() {
		assertEquals("aaa", create("aaa").lastLiteralPart());
	}

	@Test
	public void testEmptyFirstPart() {
		assertEquals("", create("@@table.col1@@aaa").firstLiteralPart());
	}
	
	@Test
	public void testEmptyLastPart() {
		assertEquals("", create("aaa@@table.col1@@").lastLiteralPart());
	}

	@Test
	public void testFirstAndLastPart() {
		assertEquals("aaa", create("aaa@@table.col1@@bbb").firstLiteralPart());
		assertEquals("bbb", create("aaa@@table.col1@@bbb").lastLiteralPart());
	}
	
	private TemplateValueMaker create(String pattern) {
		return Microsyntax.parsePattern(pattern);
	}
	
	private void assertPattern(String expected, String pattern) {
		TemplateValueMaker p = create(pattern);
		assertEquals(expected, p.makeValue(this.row));
	}
	
	private void assertPatternValues(TemplateValueMaker pattern, String value, Map<String,String> expectedValues) {
		assertTrue(matches(pattern, value));
		Collection<Expression> expressions = new HashSet<Expression>();
		for (String columnName: expectedValues.keySet()) {
			String columnValue = (String) expectedValues.get(columnName);
			ColumnName column = Microsyntax.parseColumn(columnName);
			expressions.add(Equality.create(
					new ColumnExpr(column), 
					Constant.create(columnValue, GenericType.CHARACTER.dataTypeFor(Vendor.SQL92))));
		}
		Expression expr = Conjunction.create(expressions);
		assertEquals(expr, pattern.valueExpression(value, table, Vendor.SQL92));
	}

	private boolean matches(TemplateValueMaker valueMaker, String value) {
		return !valueMaker.valueExpression(value, table, Vendor.SQL92).isFalse();
	}

	private ResultRow row(String spec) {
		String[] parts = spec.split("\\|", -1);
		ColumnName[] columns = {col1, col2, col3, col4, col5};
		Map<ColumnName,String> result = new HashMap<ColumnName,String>();
		for (int i = 0; i < parts.length && i < columns.length; i++) {
			result.put(columns[i], parts[i]);
		}
		return new ResultRow(result);
	}
}
