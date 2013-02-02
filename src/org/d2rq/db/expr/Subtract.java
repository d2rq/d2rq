package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;



public class Subtract extends BinaryOperator {

	public Subtract(Expression expr1, Expression expr2) {
		super(expr1, expr2, "-", false, GenericType.NUMERIC);
	}
	
	@Override
	protected Expression clone(Expression newOperand1, Expression newOperand2) {
		return new Subtract(newOperand1, newOperand2);
	}
}
