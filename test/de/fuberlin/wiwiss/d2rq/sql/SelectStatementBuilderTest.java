package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.TestCase;

/**
 * Unit tests for {@link SelectStatementBuilder}.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilderTest.java,v 1.1 2006/08/28 21:33:40 cyganiak Exp $
 */
public class SelectStatementBuilderTest extends TestCase {

	public void testEscape() {
		assertEquals("a", SelectStatementBuilder.escape("a"));
		assertEquals("\\'", SelectStatementBuilder.escape("'"));
		assertEquals("\\\\", SelectStatementBuilder.escape("\\"));
		assertEquals("Joe\\'s", SelectStatementBuilder.escape("Joe's"));
		assertEquals("\\\\\\'\\\\\\'\\\\",
				SelectStatementBuilder.escape("\\'\\'\\"));
	}

	public void testEmptyBuilder() {
		SelectStatementBuilder builder = new SelectStatementBuilder(null);
		assertEquals("SELECT 1", builder.getSQLStatement());
	}
}
