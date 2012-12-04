package org.d2rq.db.vendor;

import static org.junit.Assert.assertEquals;

import org.d2rq.db.DummyDB;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.vendor.Vendor;
import org.junit.Test;


/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class VendorTest {
	private Vendor vendor;
	
	@Test
	public void testSingleQuoteEscapeMySQL() {
		vendor = Vendor.MySQL;
		assertEquals("'a'", vendor.quoteStringLiteral("a"));
		assertEquals("''''", vendor.quoteStringLiteral("'"));
		assertEquals("'\\\\'", vendor.quoteStringLiteral("\\"));
		assertEquals("'Joe''s'", vendor.quoteStringLiteral("Joe's"));
		assertEquals("'\\\\''\\\\''\\\\'", vendor.quoteStringLiteral("\\'\\'\\"));
		assertEquals("'\"'", vendor.quoteStringLiteral("\""));
		assertEquals("'`'", vendor.quoteStringLiteral("`"));
	}

	@Test
	public void testSingleQuoteEscape() {
		vendor = Vendor.SQL92;
		assertEquals("'a'", vendor.quoteStringLiteral("a"));
		assertEquals("''''", vendor.quoteStringLiteral("'"));
		assertEquals("'\\'", vendor.quoteStringLiteral("\\"));
		assertEquals("'Joe''s'", vendor.quoteStringLiteral("Joe's"));
		assertEquals("'\\''\\''\\'", vendor.quoteStringLiteral("\\'\\'\\"));
		assertEquals("'\"'", vendor.quoteStringLiteral("\""));
		assertEquals("'`'", vendor.quoteStringLiteral("`"));
	}

	@Test
	public void testQuoteIdentifierEscape() {
		vendor = Vendor.SQL92;
		assertEquals("\"a\"", quoteIdentifier("a"));
		assertEquals("\"'\"", quoteIdentifier("'"));
		assertEquals("\"\"\"\"", quoteIdentifier("\""));
		assertEquals("\"`\"", quoteIdentifier("`"));
		assertEquals("\"\\\"", quoteIdentifier("\\"));
		assertEquals("\"A \"\"good\"\" idea\"", quoteIdentifier("A \"good\" idea"));
	}

	@Test
	public void testQuoteIdentifierEscapeMySQL() {
		vendor = Vendor.MySQL;
		assertEquals("`a`", quoteIdentifier("a"));
		assertEquals("````", quoteIdentifier("`"));
		assertEquals("`\\\\`", quoteIdentifier("\\"));
		assertEquals("`Joe``s`", quoteIdentifier("Joe`s"));
		assertEquals("`\\\\``\\\\``\\\\`", quoteIdentifier("\\`\\`\\"));
		assertEquals("`'`", quoteIdentifier("'"));
	}

	@Test
	public void testColumnNameQuoting() {
		vendor = Vendor.SQL92;
		assertEquals("schema.table.column",
				vendor.toString(ColumnName.parse("schema.table.column")));
		assertEquals("table.column",
				vendor.toString(ColumnName.parse("table.column")));
		assertEquals("\"schema\".\"table\".\"column\"",
				vendor.toString(ColumnName.parse("\"schema\".\"table\".\"column\"")));
		assertEquals("\"table\".\"column\"",
				vendor.toString(ColumnName.parse("\"table\".\"column\"")));
	}
	
	@Test
	public void testDoubleQuotesInColumnNamesAreEscaped() {
		vendor = Vendor.SQL92;
		assertEquals("\"sch\"\"ema\".\"ta\"\"ble\".\"col\"\"umn\"",
				vendor.toString(ColumnName.create(null,
						Identifier.createDelimited("sch\"ema"),
						Identifier.createDelimited("ta\"ble"),
						Identifier.createDelimited("col\"umn"))));
	}
	
	@Test
	public void testColumnNameQuotingMySQL() {
		vendor = Vendor.MySQL;
		assertEquals("`table`.`column`",
				vendor.toString(ColumnName.parse("\"table\".\"column\"")));
	}

	@Test
	public void testTableNameQuoting() {
		vendor = new DummyDB().vendor();
		assertEquals("schema.table",
				vendor.toString(TableName.parse("schema.table")));
		assertEquals("table",
				vendor.toString(TableName.parse("table")));
		assertEquals("\"schema\".\"table\"",
				vendor.toString(TableName.parse("\"schema\".\"table\"")));
		assertEquals("\"table\"",
				vendor.toString(TableName.parse("\"table\"")));
	}
	
	@Test
	public void testBackticksInRelationsAreEscapedMySQL() {
		vendor = Vendor.MySQL;
		assertEquals("`ta``ble`",
				vendor.toString(TableName.create(null, null, 
						Identifier.createDelimited("ta`ble"))));
	}
	
	@Test
	public void testTableNameQuotingMySQL() {
		vendor = Vendor.MySQL;
		assertEquals("`table`",
				vendor.toString(TableName.parse("\"table\"")));
	}

	private String quoteIdentifier(String identifier) {
		return vendor.toString(Identifier.createDelimited(identifier));
	}
}
