package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class UnaryMinus extends UnaryExpression {
	
	public UnaryMinus(Expression operand) {
		super("-", operand, GenericType.NUMERIC);
	}

	@Override
	public Expression clone(Expression newOperand) {
		return new UnaryMinus(newOperand);
	}
	
	@Override
	public String toSQL(String operandSQL, Vendor vendor) {
		return "-(" + operandSQL + ")";
	}	
}
