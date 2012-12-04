package org.d2rq.db.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Test;

public class ConjunctionTest {
	private final static String sql1 = "papers.publish = 1";
	private final static String sql2 = "papers.rating > 4";
	private final static String sql3 = "papers.reviewed = 1";
	private final static Expression expr1 = SQLExpression.create(sql1, GenericType.BOOLEAN);
	private final static Expression expr2 = SQLExpression.create(sql2, GenericType.BOOLEAN);
	private final static Expression expr3 = SQLExpression.create(sql3, GenericType.BOOLEAN);
	private final static Expression conjunction12 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr1, expr2}));
	private final static Expression conjunction123 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr1, expr2, expr3}));
	private final static Expression conjunction21 = 
		Conjunction.create(Arrays.asList(new Expression[]{expr2, expr1}));

	@Test
	public void testEmptyConjunctionIsTrue() {
		assertEquals(Expression.TRUE, Conjunction.create(Collections.<Expression>emptySet()));
	}
	
	@Test
	public void testSingletonConjunctionIsSelf() {
		Expression e = SQLExpression.create("foo", GenericType.CHARACTER);
		assertEquals(e, Conjunction.create(Collections.singleton(e)));
	}
	
	@Test
	public void testCreateConjunction() {
		assertFalse(conjunction12.isTrue());
		assertFalse(conjunction12.isFalse());
	}
	
	@Test
	public void testToString() {
		assertEquals("Conjunction(SQL(papers.publish = 1), SQL(papers.rating > 4))",
				conjunction12.toString());		
	}
	
	@Test
	public void testTrueExpressionsAreSkipped() {
		assertEquals(Expression.TRUE, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, Expression.TRUE})));
		assertEquals(expr1, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, expr1, Expression.TRUE})));
		assertEquals(conjunction12, Conjunction.create(
				Arrays.asList(new Expression[]{Expression.TRUE, expr1, Expression.TRUE, expr2})));
	}

	@Test
	public void testFalseCausesFailure() {
		assertEquals(Expression.FALSE, Conjunction.create(Collections.singleton(Expression.FALSE)));
		assertEquals(Expression.FALSE, Conjunction.create(
				Arrays.asList(new Expression[]{expr1, Expression.FALSE})));
	}

	@Test
	public void testRemoveDuplicates() {
		assertEquals(expr1, Conjunction.create(Arrays.asList(new Expression[]{expr1, expr1})));
	}
	
	@Test
	public void testFlatten() {
		assertEquals(conjunction123, Conjunction.create(
				Arrays.asList(new Expression[]{conjunction12, expr3})));
	}
	
	@Test
	public void testOrderDoesNotAffectEquality() {
		assertEquals(conjunction12, conjunction21);
		assertEquals(conjunction12.hashCode(), conjunction21.hashCode());
	}
}