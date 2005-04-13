/*
 * $Id: SQLStatementMakerTest.java,v 1.2 2005/04/13 17:18:01 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
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
