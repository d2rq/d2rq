package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;



public class Multiply extends BinaryOperator {

	public Multiply(Expression expr1, Expression expr2) {
		super(expr1, expr2, "*", true, GenericType.NUMERIC);
	}

	@Override
	protected Expression clone(Expression newOperand1, Expression newOperand2) {
		return new Multiply(newOperand1, newOperand2);
	}
}
