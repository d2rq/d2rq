package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SQLBuildingTest.java,v 1.7 2009/09/29 19:56:54 cyganiak Exp $
 */
public class SQLBuildingTest extends TestCase {
	private final static Attribute foo = new Attribute(null, "table", "foo");
	
	public void testSingleQuoteEscape() {
		ConnectedDB db = new DummyDB();
		assertEquals("'a'", db.singleQuote("a"));
		assertEquals("''''", db.singleQuote("'"));
		assertEquals("'\\\\'", db.singleQuote("\\"));
		assertEquals("'Joe''s'", db.singleQuote("Joe's"));
		assertEquals("'\\\\''\\\\''\\\\'", db.singleQuote("\\'\\'\\"));
		assertEquals("'\"'", db.singleQuote("\""));
		assertEquals("'`'", db.singleQuote("`"));
	}

	public void testSingleQuoteEscapeOracle() {
		ConnectedDB db = new DummyDB(ConnectedDB.Oracle);
		assertEquals("'a'", db.singleQuote("a"));
		assertEquals("''''", db.singleQuote("'"));
		assertEquals("'\\'", db.singleQuote("\\"));
		assertEquals("'Joe''s'", db.singleQuote("Joe's"));
		assertEquals("'\\''\\''\\'", db.singleQuote("\\'\\'\\"));
		assertEquals("'\"'", db.singleQuote("\""));
		assertEquals("'`'", db.singleQuote("`"));
	}

	public void testQuoteIdentifierEscape() {
		SQLSyntax db = new DummyDB().getSyntax();
		assertEquals("\"a\"", db.quoteIdentifier("a"));
		assertEquals("\"'\"", db.quoteIdentifier("'"));
		assertEquals("\"\"\"\"", db.quoteIdentifier("\""));
		assertEquals("\"`\"", db.quoteIdentifier("`"));
		assertEquals("\"\\\"", db.quoteIdentifier("\\"));
		assertEquals("\"A \"\"good\"\" idea\"", db.quoteIdentifier("A \"good\" idea"));
	}

	public void testQuoteIdentifierEscapeMySQL() {
		SQLSyntax db = new DummyDB(ConnectedDB.MySQL).getSyntax();
		assertEquals("`a`", db.quoteIdentifier("a"));
		assertEquals("````", db.quoteIdentifier("`"));
		assertEquals("`\\\\`", db.quoteIdentifier("\\"));
		assertEquals("`Joe``s`", db.quoteIdentifier("Joe`s"));
		assertEquals("`\\\\``\\\\``\\\\`", db.quoteIdentifier("\\`\\`\\"));
		assertEquals("`'`", db.quoteIdentifier("'"));
	}

	public void testAttributeQuoting() {
		SQLSyntax db = new DummyDB().getSyntax();
		assertEquals("\"schema\".\"table\".\"column\"",
				db.quoteAttribute(new Attribute("schema", "table", "column")));
		assertEquals("\"table\".\"column\"",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}
	
	public void testDoubleQuotesInAttributesAreEscaped() {
		SQLSyntax db = new DummyDB().getSyntax();
		assertEquals("\"sch\"\"ema\".\"ta\"\"ble\".\"col\"\"umn\"",
				db.quoteAttribute(new Attribute("sch\"ema", "ta\"ble", "col\"umn")));
	}
	
	public void testAttributeQuotingMySQL() {
		SQLSyntax db = new DummyDB(ConnectedDB.MySQL).getSyntax();
		assertEquals("`table`.`column`",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}

	public void testRelationNameQuoting() {
		SQLSyntax db = new DummyDB().getSyntax();
		assertEquals("\"schema\".\"table\"",
				db.quoteRelationName(new RelationName("schema", "table")));
		assertEquals("\"table\"",
				db.quoteRelationName(new RelationName(null, "table")));
	}
	
	public void testBackticksInRelationsAreEscapedMySQL() {
		SQLSyntax db = new DummyDB(ConnectedDB.MySQL).getSyntax();
		assertEquals("`ta``ble`",
				db.quoteRelationName(new RelationName(null, "ta`ble")));
	}
	
	public void testRelationNameQuotingMySQL() {
		SQLSyntax db = new DummyDB(ConnectedDB.MySQL).getSyntax();
		assertEquals("`table`",
				db.quoteRelationName(new RelationName(null, "table")));
	}
	
	public void testNoLimit() {
		ConnectedDB db = new DummyDB();
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT \"table\".\"foo\" FROM \"table\"",
				new SelectStatementBuilder(r).getSQLStatement());
	}
	
	public void testLimitStandard() {
		DummyDB db = new DummyDB();
		db.setLimit(100);
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT \"table\".\"foo\" FROM \"table\" LIMIT 100",
				new SelectStatementBuilder(r).getSQLStatement());
	}
	
	public void testNoLimitMSSQL() {
		DummyDB db = new DummyDB(ConnectedDB.MSSQL);
		db.setLimit(100);
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT TOP 100 \"table\".\"foo\" FROM \"table\"",
				new SelectStatementBuilder(r).getSQLStatement());
	}
	
	public void testNoLimitOracle() {
		DummyDB db = new DummyDB(ConnectedDB.Oracle);
		db.setLimit(100);
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT \"table\".\"foo\" FROM \"table\" WHERE (ROWNUM <= 100)",
				new SelectStatementBuilder(r).getSQLStatement());
	}
}
