package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.d2rq.D2RQException;
import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.renamer.ColumnRenamer;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.lang.AliasDeclaration;
import org.d2rq.lang.Microsyntax;
import org.junit.Test;


public class MicrosyntaxTest {
	private final static ColumnName foo_col1 = ColumnName.parse("\"foo\".\"col1\"");
	private final static ColumnName foo_col2 = ColumnName.parse("\"foo\".\"col2\"");
	private final static ColumnName bar_col1 = ColumnName.parse("\"bar\".\"col1\"");
	private final static ColumnName bar_col2 = ColumnName.parse("\"bar\".\"col2\"");
	private final static AliasDeclaration fooAsBar = new AliasDeclaration(
			TableName.parse("\"foo\""), TableName.parse("\"bar\""));
	
	@Test
	public void testParseTableNameNoSchema() {
		TableName n = Microsyntax.parseTable("table");
		assertEquals("table", n.getTable().getName());
		assertNull(n.getSchema());
		assertTrue(n.getTable().isDelimited());
	}
	
	@Test
	public void testParseTableNameWithSchema() {
		TableName n = Microsyntax.parseTable("schema.table");
		assertEquals("table", n.getTable().getName());
		assertEquals("schema", n.getSchema().getName());
		assertTrue(n.getTable().isDelimited());
		assertTrue(n.getSchema().isDelimited());
	}
	
	@Test
	public void testParseInvalidTableName() {
		try {
			Microsyntax.parseTable(".");
			fail();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_TABLE_NAME, ex.errorCode());
		}
	}

	@Test
	public void testParseColumnName() {
		ColumnName col = Microsyntax.parseColumn("schema.table.column");
		assertEquals("schema", col.getQualifier().getSchema().getName());
		assertTrue(col.getQualifier().getSchema().isDelimited());
		assertEquals("table", col.getQualifier().getTable().getName());
		assertTrue(col.getQualifier().getTable().isDelimited());
		assertEquals("column", col.getColumn().getName());
		assertTrue(col.getColumn().isDelimited());
	}
	
	@Test
	public void testParseInvalidColumnName() {
		try {
			Microsyntax.parseColumn("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_COLUMN_NAME, ex.errorCode());
		}
	}

	private Expression parseSQL(String expression) {
		return Microsyntax.parseSQLExpression(expression, GenericType.CHARACTER);
	}
	
	@Test
	public void testFindColumnInEmptyExpression() {
		assertEquals(Collections.EMPTY_SET, parseSQL("1+2").getColumns());
	}
	
	@Test
	public void testNumbersInExpressionsAreNotColumns() {
		assertEquals(Collections.EMPTY_SET, parseSQL("1.2").getColumns());
	}
	
	@Test
	public void testFindColumnInColumnName() {
		assertEquals(Collections.singleton(foo_col1), parseSQL("foo.col1").getColumns());
	}
	
	@Test
	public void testFindColumnsInExpression() {
		assertEquals(new HashSet<ColumnName>() {{ add(foo_col1); add(bar_col2); }},
				parseSQL("foo.col1 + bar.col2 = 135").getColumns());
	}
	
	@Test
	public void testFindColumnsInExpression2() {
		assertEquals(new HashSet<ColumnName>() {{ add(foo_col1); add(foo_col2); }},
				parseSQL("'must.not.match' = foo.col1 && foo.col2 = 'must.not' && foo.col2").getColumns());
	}

	@Test
	public void testFindColumnsInExpressionWithSchema() {
		assertEquals(new HashSet<ColumnName>(Arrays.asList(new ColumnName[]{
				Microsyntax.createColumn("s1", "t1", "c1"), 
				Microsyntax.createColumn("s2", "t2", "c2")})),
				parseSQL("s1.t1.c1 + s2.t2.c2 = 135").getColumns());
	}
	
	@Test
	public void testFindColumnsInExpressionWithStrings() {
		assertEquals(new HashSet<ColumnName>(Arrays.asList(new ColumnName[]{foo_col1, foo_col2, bar_col1})),
				parseSQL("FUNC('mustnot.match', foo.col1, 'must.not.match') = foo.col2 && FUNC(F2(), bar.col1)").getColumns());
	}

	@Test
	public void testFindColumnsInExpressionWithStrings2() { // may occur with d2rq:sqlExpression
		assertEquals(new HashSet<ColumnName>(Arrays.asList(new ColumnName[]{foo_col1})),
				parseSQL("FUNC('mustnot.match', foo.col1, 'must.not.match')").getColumns());
	}
	
	@Test
	public void testReplaceColumnsInExpressionWithTableRenamer() {
		Renamer renamer = TableRenamer.create(AliasOp.create(new DummyDB().table("\"foo\""), "\"bar\""));
		assertEquals("SQL(\"bar\".\"col1\")", 
				renamer.applyTo(parseSQL("foo.col1")).toString());
		assertEquals("SQL(LEN(\"bar\".\"col1\") > 0)", 
				renamer.applyTo(parseSQL("LEN(foo.col1) > 0")).toString());
		assertEquals("SQL(\"baz\".\"col1\")", 
				renamer.applyTo(parseSQL("baz.col1")).toString());
		assertEquals("SQL(\"fooo\".\"col1\")", 
				renamer.applyTo(parseSQL("fooo.col1")).toString());
		assertEquals("SQL(\"ofoo\".\"col1\")", 
				renamer.applyTo(parseSQL("ofoo.col1")).toString());
	}
	
	@Test
	public void testReplaceColumnsInExpressionWithColumnRenamer() {
		ColumnRenamer renamer = new ColumnRenamer(Collections.singletonMap(foo_col1, bar_col2));
		assertEquals("SQL(\"bar\".\"col2\")", 
				renamer.applyTo(parseSQL("foo.col1")).toString());
		assertEquals("SQL(LEN(\"bar\".\"col2\") > 0)", 
				renamer.applyTo(parseSQL("LEN(foo.col1) > 0")).toString());
		assertEquals("SQL(\"foo\".\"col3\")", 
				renamer.applyTo(parseSQL("foo.col3")).toString());
		assertEquals("SQL(\"foo\".\"col11\")", 
				renamer.applyTo(parseSQL("foo.col11")).toString());
		assertEquals("SQL(\"ofoo\".\"col1\")", 
				renamer.applyTo(parseSQL("ofoo.col1")).toString());
	}

	@Test
	public void testParseAliasIsCaseInsensitive() {
		assertEquals(fooAsBar, Microsyntax.parseAlias("foo AS bar"));
		assertEquals(fooAsBar, Microsyntax.parseAlias("foo as bar"));
	}

	@Test
	public void testParseAlias() {
		assertEquals(
				new AliasDeclaration(Microsyntax.parseTable("schema.table1"), 
						Microsyntax.parseTable("table2")),
				Microsyntax.parseAlias("schema.table1 AS table2"));
	}
	
	@Test
	public void testParseInvalidAlias() {
		try {
			Microsyntax.parseAlias("asdf");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_ALIAS, ex.errorCode());
		}
	}
	
	@Test
	public void testParseColumnList() {
		assertEquals(Arrays.asList(new ColumnName[]{foo_col1, foo_col2}),
				Microsyntax.parseColumnList("foo.col1, foo.col2"));
	}
}
