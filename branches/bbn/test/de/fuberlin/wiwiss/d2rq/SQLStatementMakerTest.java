/*
 * $Id: SQLStatementMakerTest.java,v 1.1 2006/04/12 09:56:16 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.utils.StringUtils;
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
		assertEquals("a", StringUtils.escape("a"));
		assertEquals("\\'", StringUtils.escape("'"));
		assertEquals("\\\\", StringUtils.escape("\\"));
		assertEquals("Joe\\'s", StringUtils.escape("Joe's"));
		assertEquals("\\\\\\'\\\\\\'\\\\",
		        StringUtils.escape("\\'\\'\\"));
	}
}
