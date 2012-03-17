package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class ConjunctionTest extends TestCase {
	private final static String sql1 = "papers.publish = 1";
	private final static String sql2 = "papers.rating > 4";
	private final static String sql3 = "papers.reviewed = 1";
	private final static Expression expr1 = SQLExpression.create(sql1);
	private final static Expression expr2 = SQLExpression.create(sql2);
	private final static Expression expr3 = SQLExpression.create(sql3);
	private final static Expression conjunction12 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr1, expr2}));
	private final static Expression conjunction123 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr1, expr2, expr3}));
	private final static Expression conjunction21 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr2, expr1}));

	public void testEmptyConjunctionIsTrue() {
		assertEquals(Expression.TRUE, Conjunction.create(Collections.<Expression>emptySet()));
	}
	
	public void testSingletonConjunctionIsSelf() {
		Expression e = SQLExpression.create("foo");
		assertEquals(e, Conjunction.create(Collections.singleton(e)));
	}
	
	public void testCreateConjunction() {
		assertFalse(conjunction12.isTrue());
		assertFalse(conjunction12.isFalse());
	}
	
	public void testToString() {
		assertEquals("Conjunction(SQL(papers.publish = 1), SQL(papers.rating > 4))",
				conjunction12.toString());		
	}
	
	public void testTrueExpressionsAreSkipped() {
		assertEquals(Expression.TRUE, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, Expression.TRUE})));
		assertEquals(expr1, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, expr1, Expression.TRUE})));
		assertEquals(conjunction12, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, expr1, Expression.TRUE, expr2})));
	}

	public void testFalseCausesFailure() {
		assertEquals(Expression.FALSE, Conjunction.create(Collections.singleton(Expression.FALSE)));
		assertEquals(Expression.FALSE, Conjunction.create(
				Arrays.asList(new Expression[]{expr1, Expression.FALSE})));
	}

	public void testRemoveDuplicates() {
		assertEquals(expr1, Conjunction.create(Arrays.asList(new Expression[]{expr1, expr1})));
	}
	
	public void testFlatten() {
		assertEquals(conjunction123, Conjunction.create(
				Arrays.asList(new Expression[]{conjunction12, expr3})));
	}
	
	public void testOrderDoesNotAffectEquality() {
		assertEquals(conjunction12, conjunction21);
		assertEquals(conjunction12.hashCode(), conjunction21.hashCode());
	}
}