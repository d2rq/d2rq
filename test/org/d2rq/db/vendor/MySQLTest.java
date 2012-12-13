package org.d2rq.db.vendor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.schema.Identifier.ViolationType;
import org.junit.Test;

public class MySQLTest {

	private Identifier[] parseSQL92(String identifiers) 
	throws IdentifierParseException {
		return Vendor.SQL92.parseIdentifiers(identifiers, 0, 100);
	}
	
	private Identifier[] parseMySQL(String identifiers) 
	throws IdentifierParseException {
		return Vendor.MySQL.parseIdentifiers(identifiers, 0, 100);
	}
	
	@Test
	public void testParseIdentifierSimple() throws IdentifierParseException {
		assertArrayEquals(parseSQL92("foo"), parseMySQL("foo"));
		assertArrayEquals(parseSQL92("FOO"), parseMySQL("FOO"));
		assertArrayEquals(parseSQL92("foo.bar"), parseMySQL("foo.bar"));
		assertArrayEquals(parseSQL92("FOO.BAR"), parseMySQL("FOO.BAR"));
	}
	
	@Test
	public void testParseIdentifierDelimited() throws IdentifierParseException {
		assertArrayEquals(parseSQL92("\"foo\""), parseMySQL("`foo`"));
		assertArrayEquals(parseSQL92("\"foo\".\"bar\""), parseMySQL("`foo`.`bar`"));
	}
	
	@Test
	public void testParseIdentifierEscapes() throws IdentifierParseException {
		assertArrayEquals(parseSQL92("\"fo`o\""), parseMySQL("`fo``o`"));
		assertArrayEquals(parseSQL92("\"fo\"\"o\""), parseMySQL("`fo\"o`"));
	}
	
	@Test
	public void testParseIdentifierWithDollarSign() throws IdentifierParseException {
		assertEquals("A$B", parseMySQL("A$B")[0].getName());
	}

	@Test
	public void testParseIdentifierEndingWithSpace() throws IdentifierParseException {
		parseSQL92("\"foo \"");
		try {
			parseMySQL("`foo `");
			fail("Should throw exception as identifiers cannot end in space in MySQL");
		} catch (IdentifierParseException ex) {
			assertEquals(ViolationType.UNEXPECTED_CHARACTER, ex.getViolationType());
		}
	}
}
