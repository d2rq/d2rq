package de.fuberlin.wiwiss.d2rq.expr;

import junit.framework.TestCase;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ExpressionTest.java,v 1.1 2006/11/02 20:46:46 cyganiak Exp $
 */
public class ExpressionTest extends TestCase {

	public void testTrue() {
		assertEquals("TRUE", Expression.TRUE.toString());
		assertEquals(Expression.TRUE, Expression.TRUE);
		assertTrue(Expression.TRUE.isTrue());
		assertFalse(Expression.TRUE.isFalse());
	}
	
	public void testFalse() {
		assertEquals("FALSE", Expression.FALSE.toString());
		assertEquals(Expression.FALSE, Expression.FALSE);
		assertFalse(Expression.FALSE.isTrue());
		assertTrue(Expression.FALSE.isFalse());
	}
	
	public void testTrueNotEqualFalse() {
		assertFalse(Expression.TRUE.equals(Expression.FALSE));
	}
}
