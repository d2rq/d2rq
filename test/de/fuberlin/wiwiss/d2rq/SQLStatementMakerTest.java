/*
 * $Id: SQLStatementMakerTest.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import junit.framework.TestCase;

/**
 * Unit tests for {@link SQLStatementMaker}.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class SQLStatementMakerTest extends TestCase {

	/**
	 * Constructor for SQLStatementMakerTest.
	 * @param arg0
	 */
	public SQLStatementMakerTest(String arg0) {
		super(arg0);
	}

	public void testEscape() {
		assertEquals("a", SQLStatementMaker.escape("a"));
		assertEquals("\\'", SQLStatementMaker.escape("'"));
		assertEquals("\\\\", SQLStatementMaker.escape("\\"));
		assertEquals("Joe\\'s", SQLStatementMaker.escape("Joe's"));
		assertEquals("\\\\\\'\\\\\\'\\\\",
				SQLStatementMaker.escape("\\'\\'\\"));
	}
}
