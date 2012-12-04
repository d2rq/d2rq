package org.d2rq.db.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.renamer.ColumnRenamer;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.lang.Microsyntax;
import org.junit.Test;


public class SQLExpressionTest {

	@Test
	public void testCreate() {
		Expression e = SQLExpression.create("papers.publish = 1", GenericType.BOOLEAN);
		assertEquals("SQL(papers.publish = 1)", e.toString());
		assertFalse(e.isTrue());
		assertFalse(e.isFalse());
	}
	
	@Test
	public void testFindsColumns() {
		Expression e = Microsyntax.parseSQLExpression("papers.publish = 1 AND papers.url1 != 'http://www.example.com\\'http://www.example.com' AND papers.url2 != 'http://www.example.com\\\\\\\\http://www.example.com' AND papers.rating > 4", GenericType.BOOLEAN);
		Set<ColumnName> expectedColumns = new HashSet<ColumnName>(Arrays.asList(
				new ColumnName[]{Microsyntax.parseColumn("papers.publish"), 
						Microsyntax.parseColumn("papers.url1"),
						Microsyntax.parseColumn("papers.url2"),
						Microsyntax.parseColumn("papers.rating")}));
		assertEquals(expectedColumns, e.getColumns());
	}
	
	@Test
	public void testToString() {
		Expression e = Microsyntax.parseSQLExpression("papers.publish = 1", GenericType.BOOLEAN);
		assertEquals("SQL(\"papers\".\"publish\" = 1)", e.toString());
	}
	
	@Test
	public void testTwoExpressionsAreEqual() {
		assertEquals(SQLExpression.create("1=1", GenericType.BOOLEAN), SQLExpression.create("1=1", GenericType.BOOLEAN));
		assertEquals(SQLExpression.create("1=1", GenericType.BOOLEAN).hashCode(), SQLExpression.create("1=1", GenericType.BOOLEAN).hashCode());
	}
	
	@Test
	public void testTwoExpressionsAreNotEqual() {
		assertFalse(SQLExpression.create("1=1", GenericType.BOOLEAN).equals(SQLExpression.create("2=2", GenericType.BOOLEAN)));
		assertFalse(SQLExpression.create("1=1", GenericType.BOOLEAN).hashCode() == SQLExpression.create("2=2", GenericType.BOOLEAN).hashCode());
	}
	
	@Test
	public void testColumnEquality() {
		ColumnName col1a = ColumnName.parse("COL1");
		ColumnName col1b = ColumnName.parse("\"COL1\"");
		assertEquals(col1a, col1b);
		assertEquals(
				SQLExpression.create(Arrays.asList(new String[]{"", ""}), Collections.singletonList(col1a), GenericType.CHARACTER),
				SQLExpression.create(Arrays.asList(new String[]{"", ""}), Collections.singletonList(col1b), GenericType.CHARACTER));
	}
	
	@Test
	public void testRenameColumnsWithAliasMap() {
		AliasOp a = AliasOp.create(DummyDB.createTable("FOO"), "BAR");
		assertEquals(Microsyntax.parseSQLExpression("BAR.col1 = BAZ.col1", GenericType.BOOLEAN),
				Microsyntax.parseSQLExpression("FOO.col1 = BAZ.col1", GenericType.BOOLEAN).rename(
						TableRenamer.create(a)));
	}
	
	@Test
	public void testRenameColumnsWithColumnReplacer() {
		Map<ColumnName,ColumnName> map = new HashMap<ColumnName,ColumnName>();
		map.put(Microsyntax.parseColumn("foo.col1"), Microsyntax.parseColumn("foo.col2"));
		assertEquals(Microsyntax.parseSQLExpression("foo.col2=foo.col3", GenericType.BOOLEAN), 
				Microsyntax.parseSQLExpression("foo.col1=foo.col3", GenericType.BOOLEAN).rename(new ColumnRenamer(map)));
	}
}
