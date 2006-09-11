package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.map.Column;

/**
 * Unit tests for {@link SelectStatementBuilder}.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilderTest.java,v 1.4 2006/09/11 22:29:21 cyganiak Exp $
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
		assertTrue(builder.isTrivial());
	}
	
	public void testConditionWithNoSelectColumnsIsNotTrivial() {
		SelectStatementBuilder builder = new SelectStatementBuilder(null);
		builder.addCondition(new Expression("foo.bar = 1"));
		assertFalse(builder.isTrivial());
	}

	public void testQueryWithSelectColumnsIsNotTrivial() {
		SelectStatementBuilder builder = new SelectStatementBuilder(null);
		builder.addSelectColumn(new Column("foo.bar"));
		assertFalse(builder.isTrivial());
	}
}
