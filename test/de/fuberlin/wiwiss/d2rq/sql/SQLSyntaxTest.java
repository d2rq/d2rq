package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamerMap;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

public class SQLSyntaxTest extends TestCase {
	private final static Attribute foo_col1 = new Attribute(null, "foo", "col1");
	private final static Attribute foo_col2 = new Attribute(null, "foo", "col2");
	private final static Attribute bar_col1 = new Attribute(null, "bar", "col1");
	private final static Attribute bar_col2 = new Attribute(null, "bar", "col2");
	private final static Attribute baz_col1 = new Attribute(null, "baz", "col1");
	private final static Alias fooAsBar = new Alias(
			new RelationName(null, "foo"),
			new RelationName(null, "bar"));
	
	public void testParseRelationNameNoSchema() {
		RelationName r = SQL.parseRelationName("table");
		assertEquals("table", r.tableName());
		assertNull(r.schemaName());
	}
	
	public void testParseRelationNameWithSchema() {
		RelationName r = SQL.parseRelationName("schema.table");
		assertEquals("table", r.tableName());
		assertEquals("schema", r.schemaName());
	}
	
	public void testParseInvalidRelationName() {
		try {
			SQL.parseRelationName(".");
			fail();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_RELATIONNAME, ex.errorCode());
		}
	}

	public void testParseInvalidAttributeName() {
		try {
			SQL.parseAttribute("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_ATTRIBUTENAME, ex.errorCode());
		}
	}

	public void testFindColumnInEmptyExpression() {
		assertEquals(Collections.EMPTY_SET, SQL.findColumnsInExpression("1+2"));
	}
	
	public void testNumbersInExpressionsAreNotColumns() {
		assertEquals(Collections.EMPTY_SET, SQL.findColumnsInExpression("1.2"));
	}
	
	public void testFindColumnInColumnName() {
		assertEquals(Collections.singleton(foo_col1),
				SQL.findColumnsInExpression("foo.col1"));
	}
	
	public void testFindColumnsInExpression() {
		assertEquals(new HashSet<Attribute>(Arrays.asList(new Attribute[]{foo_col1, bar_col2})),
				SQL.findColumnsInExpression("foo.col1 + bar.col2 = 135"));
	}
	
	public void testFindColumnsInExpression2() {
		assertEquals(new HashSet<Attribute>(Arrays.asList(new Attribute[]{foo_col1, foo_col2})),
				SQL.findColumnsInExpression("'must.not.match' = foo.col1 && foo.col2 = 'must.not' && foo.col2"));
	}

	public void testFindColumnsInExpressionWithSchema() {
		assertEquals(new HashSet<Attribute>(Arrays.asList(new Attribute[]{
				new Attribute("s1", "t1", "c1"), 
				new Attribute("s2", "t2", "c2")})),
				SQL.findColumnsInExpression("s1.t1.c1 + s2.t2.c2 = 135"));
	}
	
	public void testFindColumnsInExpressionWithStrings() {
		assertEquals(new HashSet<Attribute>(Arrays.asList(new Attribute[]{foo_col1, foo_col2, bar_col1})),
				SQL.findColumnsInExpression("FUNC('mustnot.match', foo.col1, 'must.not.match') = foo.col2 && FUNC(F2(), bar.col1)"));
	}

	public void testFindColumnsInExpressionWithStrings2() { // may occur with d2rq:sqlExpression
		assertEquals(new HashSet<Attribute>(Arrays.asList(new Attribute[]{foo_col1})),
				SQL.findColumnsInExpression("FUNC('mustnot.match', foo.col1, 'must.not.match')"));
	}
	
	public void testReplaceColumnsInExpressionWithAliasMap() {
		Alias alias = new Alias(new RelationName(null, "foo"), new RelationName(null, "bar"));
		AliasMap fooAsBar = new AliasMap(Collections.singleton(alias));
		assertEquals("bar.col1", 
				SQL.replaceColumnsInExpression("foo.col1", fooAsBar));
		assertEquals("LEN(bar.col1) > 0", 
				SQL.replaceColumnsInExpression("LEN(foo.col1) > 0", fooAsBar));
		assertEquals("baz.col1", 
				SQL.replaceColumnsInExpression("baz.col1", fooAsBar));
		assertEquals("fooo.col1", 
				SQL.replaceColumnsInExpression("fooo.col1", fooAsBar));
		assertEquals("ofoo.col1", 
				SQL.replaceColumnsInExpression("ofoo.col1", fooAsBar));
	}
	
	public void testReplaceColumnsWithSchemaInExpressionWithAliasMap() {
		Alias alias = new Alias(new RelationName("schema", "foo"), new RelationName("schema", "bar"));
		AliasMap fooAsBar = new AliasMap(Collections.singleton(alias));
		assertEquals("schema.bar.col1", 
				SQL.replaceColumnsInExpression("schema.foo.col1", fooAsBar));
	}
	
	public void testReplaceColumnsInExpressionWithColumnReplacer() {
		Map<Attribute,Attribute> map = new HashMap<Attribute,Attribute>();
		map.put(foo_col1, bar_col2);
		ColumnRenamerMap col1ToCol2 = new ColumnRenamerMap(map);
		assertEquals("bar.col2", 
				SQL.replaceColumnsInExpression("foo.col1", col1ToCol2));
		assertEquals("LEN(bar.col2) > 0", 
				SQL.replaceColumnsInExpression("LEN(foo.col1) > 0", col1ToCol2));
		assertEquals("foo.col3", 
				SQL.replaceColumnsInExpression("foo.col3", col1ToCol2));
		assertEquals("foo.col11", 
				SQL.replaceColumnsInExpression("foo.col11", col1ToCol2));
		assertEquals("ofoo.col1", 
				SQL.replaceColumnsInExpression("ofoo.col1", col1ToCol2));
	}

	public void testParseAliasIsCaseInsensitive() {
		assertEquals(fooAsBar, SQL.parseAlias("foo AS bar"));
		assertEquals(fooAsBar, SQL.parseAlias("foo as bar"));
	}

	public void testParseAlias() {
		assertEquals(
				new Alias(new RelationName(null, "table1"), new RelationName("schema", "table2")),
				SQL.parseAlias("table1 AS schema.table2"));
	}
	
	public void testParseInvalidAlias() {
		try {
			SQL.parseAlias("asdf");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_ALIAS, ex.errorCode());
		}
	}
	
	public void testParseInvalidJoin() {
		try {
			SQL.parseJoins(Collections.singleton("asdf"));
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_JOIN, ex.errorCode());
		}
	}
	
	public void testParseJoinOneCondition() {
		Set<Join> joins = SQL.parseJoins(Collections.singleton("foo.col1 = bar.col2"));
		assertEquals(1, joins.size());
		Join join = (Join) joins.iterator().next();
		assertEquals(Collections.singletonList(bar_col2), join.attributes1());
		assertEquals(Collections.singletonList(foo_col1), join.attributes2());
	}
	
	public void testParseJoinTwoConditionsOnSameTables() {
		Set<Join> joins = SQL.parseJoins(Arrays.asList(new String[]{
				"foo.col1 = bar.col1", "foo.col2 = bar.col2"}));
		assertEquals(1, joins.size());
		Join join = (Join) joins.iterator().next();
		assertEquals(Arrays.asList(new Attribute[]{bar_col1, bar_col2}),
				join.attributes1());
		assertEquals(Arrays.asList(new Attribute[]{foo_col1, foo_col2}),
				join.attributes2());
		assertEquals(foo_col1, join.equalAttribute(bar_col1));
	}
	
	public void testParseJoinTwoConditionsOnDifferentTables() {
		Set<Join> joins = SQL.parseJoins(Arrays.asList(new String[]{
				"foo.col1 <= bar.col1", "foo.col2 => baz.col1", "foo.col2 = bar.col1"}));
		assertEquals(3, joins.size());
		assertEquals(new HashSet<Join>(Arrays.asList(new Join[]{
				new Join(bar_col1, foo_col1, Join.DIRECTION_LEFT),
				new Join(baz_col1, foo_col2, Join.DIRECTION_RIGHT),
				new Join(foo_col2, bar_col1, Join.DIRECTION_UNDIRECTED)})),
				joins);
	}
}
