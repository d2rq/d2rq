package org.d2rq.db.expr;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Concatenation;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Test;

public class ConcatenationTest {

	@Test
	public void testCreateEmpty() {
		assertEquals(Constant.create("", GenericType.CHARACTER), Concatenation.create(Collections.<Expression>emptyList()));
	}
	
	@Test
	public void testCreateOnePart() {
		Expression expr = new ColumnExpr(ColumnName.parse("table.col"));
		assertEquals(expr, Concatenation.create(Collections.singletonList(expr)));
	}
	
	@Test
	public void testTwoParts() {
		Expression expr1 = Constant.create("mailto:", GenericType.CHARACTER);
		Expression expr2 = new ColumnExpr(ColumnName.parse("user.email"));
		Expression concat = Concatenation.create(Arrays.asList(new Expression[]{expr1, expr2}));
		assertEquals("Concatenation(Constant(mailto:@CHARACTER), ColumnExpr(user.email))",
				concat.toString());
	}
	
	@Test
	public void testFilterEmptyParts() {
		Expression empty = Constant.create("", GenericType.CHARACTER);
		Expression expr1 = Constant.create("aaa", GenericType.CHARACTER);
		assertEquals(expr1, Concatenation.create(Arrays.asList(
				new Expression[]{empty, empty, expr1, empty})));
	}
}
