package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;

public abstract class NAryOperator extends NAryExpression {

	private String operator;
	
	public NAryOperator(String operator, String name, Expression[] operands,
			boolean isCommutative, GenericType dataType) {
		super(name, operands, isCommutative, dataType);
		this.operator = operator;
	}
	
	@Override
	protected String toSQL(String[] sqlFragments, Vendor vendor) {
		StringBuffer result = new StringBuffer();
		result.append('(');
		for (int i = 0; i < sqlFragments.length; i++) {
			result.append(sqlFragments[i]);
			if (i < sqlFragments.length - 1) {
				result.append(' ');
				result.append(operator);
				result.append(' ');
			}
		}
		result.append(')');
		return result.toString();
	}
}
