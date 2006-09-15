package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SQLBuildingTest.java,v 1.1 2006/09/15 12:25:25 cyganiak Exp $
 */
public class SQLBuildingTest extends TestCase {

	public void testSingleQuoteEscape() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Other);
		assertEquals("'a'", db.singleQuote("a"));
		assertEquals("''''", db.singleQuote("'"));
		assertEquals("'\\\\'", db.singleQuote("\\"));
		assertEquals("'Joe''s'", db.singleQuote("Joe's"));
		assertEquals("'\\\\''\\\\''\\\\'", db.singleQuote("\\'\\'\\"));
		assertEquals("'\"'", db.singleQuote("\""));
		assertEquals("'`'", db.singleQuote("`"));
	}

	public void testSingleQuoteEscapeOracle() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Oracle);
		assertEquals("'a'", db.singleQuote("a"));
		assertEquals("''''", db.singleQuote("'"));
		assertEquals("'\\'", db.singleQuote("\\"));
		assertEquals("'Joe''s'", db.singleQuote("Joe's"));
		assertEquals("'\\''\\''\\'", db.singleQuote("\\'\\'\\"));
		assertEquals("'\"'", db.singleQuote("\""));
		assertEquals("'`'", db.singleQuote("`"));
	}

	public void testDoubleQuoteEscape() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Other);
		assertEquals("\"a\"", db.doubleQuote("a"));
		assertEquals("\"'\"", db.doubleQuote("'"));
		assertEquals("\"\"\"\"", db.doubleQuote("\""));
		assertEquals("\"`\"", db.doubleQuote("`"));
		assertEquals("\"\\\"", db.doubleQuote("\\"));
		assertEquals("\"A \"\"good\"\" idea\"", db.doubleQuote("A \"good\" idea"));
	}

	public void testBacktickQuoteEscape() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.MySQL);
		assertEquals("`a`", db.backtickQuote("a"));
		assertEquals("````", db.backtickQuote("`"));
		assertEquals("`\\\\`", db.backtickQuote("\\"));
		assertEquals("`Joe``s`", db.backtickQuote("Joe`s"));
		assertEquals("`\\\\``\\\\``\\\\`", db.backtickQuote("\\`\\`\\"));
		assertEquals("`'`", db.backtickQuote("'"));
	}

	public void testAttributeQuoting() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Other);
		assertEquals("\"schema\".\"table\".\"column\"",
				db.quoteAttribute(new Attribute("schema", "table", "column")));
		assertEquals("\"table\".\"column\"",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}
	
	public void testDoubleQuotesInAttributesAreEscaped() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Other);
		assertEquals("\"sch\"\"ema\".\"ta\"\"ble\".\"col\"\"umn\"",
				db.quoteAttribute(new Attribute("sch\"ema", "ta\"ble", "col\"umn")));
	}
	
	public void testAttributeQuotingMySQL() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.MySQL);
		assertEquals("`table`.`column`",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}

	public void testRelationNameQuoting() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.Other);
		assertEquals("\"schema\".\"table\"",
				db.quoteRelationName(new RelationName("schema", "table")));
		assertEquals("\"table\"",
				db.quoteRelationName(new RelationName(null, "table")));
	}
	
	public void testBackticksInRelationsAreEscapedMySQL() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.MySQL);
		assertEquals("`ta``ble`",
				db.quoteRelationName(new RelationName(null, "ta`ble")));
	}
	
	public void testRelationNameQuotingMySQL() {
		ConnectedDB db = createFakeDatabase(ConnectedDB.MySQL);
		assertEquals("`table`",
				db.quoteRelationName(new RelationName(null, "table")));
	}

	public void testEmptyBuilder() {
		SelectStatementBuilder builder = new SelectStatementBuilder(createFakeDatabase());
		assertNull(builder.getSQLStatement());
		assertTrue(builder.isTrivial());
	}
	
	public void testConditionWithNoSelectColumnsIsNotTrivial() {
		SelectStatementBuilder builder = new SelectStatementBuilder(createFakeDatabase());
		builder.addCondition(new Expression("foo.bar = 1"));
		assertFalse(builder.isTrivial());
	}

	public void testQueryWithSelectColumnsIsNotTrivial() {
		SelectStatementBuilder builder = new SelectStatementBuilder(createFakeDatabase());
		builder.addSelectColumn(new Attribute("foo.bar"));
		assertFalse(builder.isTrivial());
	}
	
	private ConnectedDB createFakeDatabase() {
		return createFakeDatabase(ConnectedDB.Other);
	}
	
	private ConnectedDB createFakeDatabase(final String type) {
		return new ConnectedDB(null, null, null) {
			public String dbType() {
				return type;
			}
		};
	}
}
