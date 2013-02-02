package org.d2rq.db.expr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class Concatenation extends NAryExpression {

	public static Expression create(Expression... expressions) {
		return Concatenation.create(Arrays.asList(expressions));
	}
	
	public static Expression create(List<Expression> expressions) {
		List<Expression> nonEmpty = new ArrayList<Expression>(expressions.size());
		for (Expression expression: expressions) {
			if (expression instanceof Constant 
					&& "".equals(((Constant) expression).value())) {
				continue;
			}
			nonEmpty.add(expression);
		}
		if (nonEmpty.isEmpty()) {
			return Constant.create("", GenericType.CHARACTER);
		}
		if (nonEmpty.size() == 1) {
			return nonEmpty.get(0);
		}
		return new Concatenation(nonEmpty.toArray(new Expression[nonEmpty.size()]));
	}

	private Concatenation(Expression[] parts) {
		super("Concatenation", parts, false, GenericType.CHARACTER);
	}
	
	@Override
	protected Expression clone(Expression[] newParts) {
		return new Concatenation(newParts);
	}
	
	@Override
	protected String toSQL(String[] sqlFragments, Vendor vendor) {
		return vendor.getConcatenationExpression(sqlFragments);
	}
	
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfConstantValue) return false;
		for (Expression expression: getOperands()) {
			if (expression.isConstant()) continue;
			if (expression.isConstantColumn(column, false, false, true)) continue;
			return false;
		}
		return true;
	}
}
