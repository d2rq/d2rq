package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLBuildingTest extends TestCase {
	private final static Attribute foo = new Attribute(null, "table", "foo");
	
	public void testSingleQuoteEscapeMySQL() {
		Vendor vendor = Vendor.MySQL;
		assertEquals("'a'", vendor.quoteStringLiteral("a"));
		assertEquals("''''", vendor.quoteStringLiteral("'"));
		assertEquals("'\\\\'", vendor.quoteStringLiteral("\\"));
		assertEquals("'Joe''s'", vendor.quoteStringLiteral("Joe's"));
		assertEquals("'\\\\''\\\\''\\\\'", vendor.quoteStringLiteral("\\'\\'\\"));
		assertEquals("'\"'", vendor.quoteStringLiteral("\""));
		assertEquals("'`'", vendor.quoteStringLiteral("`"));
	}

	public void testSingleQuoteEscape() {
		Vendor vendor = Vendor.SQL92;
		assertEquals("'a'", vendor.quoteStringLiteral("a"));
		assertEquals("''''", vendor.quoteStringLiteral("'"));
		assertEquals("'\\'", vendor.quoteStringLiteral("\\"));
		assertEquals("'Joe''s'", vendor.quoteStringLiteral("Joe's"));
		assertEquals("'\\''\\''\\'", vendor.quoteStringLiteral("\\'\\'\\"));
		assertEquals("'\"'", vendor.quoteStringLiteral("\""));
		assertEquals("'`'", vendor.quoteStringLiteral("`"));
	}

	public void testQuoteIdentifierEscape() {
		Vendor db = Vendor.SQL92;
		assertEquals("\"a\"", db.quoteIdentifier("a"));
		assertEquals("\"'\"", db.quoteIdentifier("'"));
		assertEquals("\"\"\"\"", db.quoteIdentifier("\""));
		assertEquals("\"`\"", db.quoteIdentifier("`"));
		assertEquals("\"\\\"", db.quoteIdentifier("\\"));
		assertEquals("\"A \"\"good\"\" idea\"", db.quoteIdentifier("A \"good\" idea"));
	}

	public void testQuoteIdentifierEscapeMySQL() {
		Vendor db = Vendor.MySQL;
		assertEquals("`a`", db.quoteIdentifier("a"));
		assertEquals("````", db.quoteIdentifier("`"));
		assertEquals("`\\\\`", db.quoteIdentifier("\\"));
		assertEquals("`Joe``s`", db.quoteIdentifier("Joe`s"));
		assertEquals("`\\\\``\\\\``\\\\`", db.quoteIdentifier("\\`\\`\\"));
		assertEquals("`'`", db.quoteIdentifier("'"));
	}

	public void testAttributeQuoting() {
		Vendor db = Vendor.SQL92;
		assertEquals("\"schema\".\"table\".\"column\"",
				db.quoteAttribute(new Attribute("schema", "table", "column")));
		assertEquals("\"table\".\"column\"",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}
	
	public void testDoubleQuotesInAttributesAreEscaped() {
		Vendor db = Vendor.SQL92;
		assertEquals("\"sch\"\"ema\".\"ta\"\"ble\".\"col\"\"umn\"",
				db.quoteAttribute(new Attribute("sch\"ema", "ta\"ble", "col\"umn")));
	}
	
	public void testAttributeQuotingMySQL() {
		Vendor db = Vendor.MySQL;
		assertEquals("`table`.`column`",
				db.quoteAttribute(new Attribute(null, "table", "column")));
	}

	public void testRelationNameQuoting() {
		Vendor db = new DummyDB().vendor();
		assertEquals("\"schema\".\"table\"",
				db.quoteRelationName(new RelationName("schema", "table")));
		assertEquals("\"table\"",
				db.quoteRelationName(new RelationName(null, "table")));
	}
	
	public void testBackticksInRelationsAreEscapedMySQL() {
		Vendor db = Vendor.MySQL;
		assertEquals("`ta``ble`",
				db.quoteRelationName(new RelationName(null, "ta`ble")));
	}
	
	public void testRelationNameQuotingMySQL() {
		Vendor db = Vendor.MySQL;
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
		DummyDB db = new DummyDB(Vendor.SQLServer);
		db.setLimit(100);
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT TOP 100 \"table\".\"foo\" FROM \"table\"",
				new SelectStatementBuilder(r).getSQLStatement());
	}
	
	public void testNoLimitOracle() {
		DummyDB db = new DummyDB(Vendor.Oracle);
		db.setLimit(100);
		Relation r = Relation.createSimpleRelation(db, new Attribute[]{foo});
		assertEquals("SELECT DISTINCT \"table\".\"foo\" FROM \"table\" WHERE (ROWNUM <= 100)",
				new SelectStatementBuilder(r).getSQLStatement());
	}
}
