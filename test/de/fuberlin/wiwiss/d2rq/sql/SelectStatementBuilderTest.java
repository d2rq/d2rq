package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;

/**
 * Unit tests for {@link SelectStatementBuilder}.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilderTest.java,v 1.2 2006/08/30 19:32:35 cyganiak Exp $
 */
public class SelectStatementBuilderTest extends TestCase {

	public void testSingleQuoteEscape() {
		assertEquals("'a'", SelectStatementBuilder.singleQuote("a"));
		assertEquals("'\\''", SelectStatementBuilder.singleQuote("'"));
		assertEquals("'\\\\'", SelectStatementBuilder.singleQuote("\\"));
		assertEquals("'Joe\\'s'", SelectStatementBuilder.singleQuote("Joe's"));
		assertEquals("'\\\\\\'\\\\\\'\\\\'",
				SelectStatementBuilder.singleQuote("\\'\\'\\"));
		assertEquals("'`'", SelectStatementBuilder.singleQuote("`"));
	}

	public void testBacktickQuoteEscape() {
		assertEquals("`a`", SelectStatementBuilder.backtickQuote("a"));
		assertEquals("````", SelectStatementBuilder.backtickQuote("`"));
		assertEquals("`\\\\`", SelectStatementBuilder.backtickQuote("\\"));
		assertEquals("`Joe``s`", SelectStatementBuilder.backtickQuote("Joe`s"));
		assertEquals("`\\\\``\\\\``\\\\`",
				SelectStatementBuilder.backtickQuote("\\`\\`\\"));
		assertEquals("`'`", SelectStatementBuilder.backtickQuote("'"));
	}

	public void testIsReservedWordWHERE() {
		assertTrue(SelectStatementBuilder.isReservedWord("WHERE"));
		assertTrue(SelectStatementBuilder.isReservedWord("where"));
		assertTrue(SelectStatementBuilder.isReservedWord("Where"));
	}
	
	public void testIsNoReservedWordFOOBAR() {
		assertFalse(SelectStatementBuilder.isReservedWord("FOOBAR"));
		assertFalse(SelectStatementBuilder.isReservedWord("foobar"));
	}
	
	public void testEmptyBuilder() {
		SelectStatementBuilder builder = new SelectStatementBuilder(null);
		assertEquals("SELECT 1", builder.getSQLStatement());
	}
}
