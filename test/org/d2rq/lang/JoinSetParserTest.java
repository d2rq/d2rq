package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.d2rq.D2RQException;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.junit.Test;


public class JoinSetParserTest {
	private final static ColumnName foo_col1 = Microsyntax.createColumn(null, "foo", "col1");
	private final static ColumnName foo_col2 = Microsyntax.createColumn(null, "foo", "col2");
	private final static ColumnName bar_col1 = Microsyntax.createColumn(null, "bar", "col1");
	private final static ColumnName bar_col2 = Microsyntax.createColumn(null, "bar", "col2");
	private final static ColumnName baz_col1 = Microsyntax.createColumn(null, "baz", "col1");
	
	@Test
	public void testParseInvalidJoin() {
		try {
			new JoinSetParser(Collections.singleton("asdf")).getExpressions();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_JOIN, ex.errorCode());
		}
	}
	
	@Test
	public void testParseJoinOneCondition() {
		Set<ColumnListEquality> joins = new JoinSetParser(
				Collections.singleton("foo.col1 = bar.col2")).getExpressions();
		assertEquals(1, joins.size());
		ColumnListEquality join = (ColumnListEquality) joins.iterator().next();
		assertEquals(Collections.singletonList(bar_col2.getColumn()), join.getColumns1().getColumns());
		assertEquals(Collections.singletonList(foo_col1.getColumn()), join.getColumns2().getColumns());
	}
	
	@Test
	public void testParseJoinTwoConditionsOnSameTables() {
		Set<ColumnListEquality> joins = new JoinSetParser(Arrays.asList(new String[]{
		"foo.col1 = bar.col1", "foo.col2 = bar.col2"})).getExpressions();
		assertEquals(1, joins.size());
		ColumnListEquality join = (ColumnListEquality) joins.iterator().next();
		assertEquals(Arrays.asList(new Identifier[]{foo_col1.getColumn(), foo_col2.getColumn()}),
				join.getColumns1().getColumns());
		assertEquals(Arrays.asList(new Identifier[]{bar_col1.getColumn(), bar_col2.getColumn()}),
				join.getColumns2().getColumns());
		assertEquals(foo_col1, join.getEqualColumn(bar_col1));
	}
	
	@Test
	public void testParseJoinTwoConditionsOnDifferentTables() {
		Set<ColumnListEquality> joins = new JoinSetParser(Arrays.asList(new String[]{
		"foo.col1 = bar.col1", "foo.col1 = baz.col1"})).getExpressions();
		assertEquals(2, joins.size());
		assertEquals(new HashSet<ColumnListEquality>(Arrays.asList(new ColumnListEquality[]{
				ColumnListEquality.create(foo_col1, bar_col1),
				ColumnListEquality.create(foo_col1, baz_col1)})),
				joins);
	}
	
	@Test
	public void testParseJoinConflictingOperators() {
		new JoinSetParser(Arrays.asList(new String[]{
		"foo.col1 => bar.col1", "foo.col2 => bar.col2"})).getExpressions();
		try {
			new JoinSetParser(Arrays.asList(new String[]{
			"foo.col1 <= bar.col1", "foo.col2 => bar.col2"})).getExpressions();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_JOIN, ex.errorCode());
		}
	}
	
	@Test
	public void testUndirectedJoinDoesNotAssertForeignKey() {
		assertTrue(new JoinSetParser(Collections.singleton("foo.col1 = bar.col2")).getAssertedForeignKeys().isEmpty());
	}
	
	@Test
	public void testAssertedForeignKeys() {
		Map<ForeignKey,TableName> actual = new JoinSetParser(Arrays.asList(new String[]{
		"foo.col1 <= bar.col1", "foo.col1 => baz.col1", "bar.col1 = baz.col1"})).getAssertedForeignKeys();
		Map<ForeignKey,TableName> expected = new HashMap<ForeignKey,TableName>();
		expected.put(new ForeignKey(Key.create(bar_col1), Key.create(foo_col1), foo_col1.getQualifier()), bar_col1.getQualifier());
		expected.put(new ForeignKey(Key.create(foo_col1), Key.create(baz_col1), baz_col1.getQualifier()), foo_col1.getQualifier());
		assertEquals(actual, expected);
	}
}
