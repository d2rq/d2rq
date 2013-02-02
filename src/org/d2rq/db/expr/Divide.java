package org.d2rq.db.expr;

import org.d2rq.db.types.DataType.GenericType;



public class Divide extends BinaryOperator {

	public Divide(Expression expr1, Expression expr2) {
		super(expr1, expr2, "/", false, GenericType.NUMERIC);
	}

	@Override
	protected Expression clone(Expression newOperand1, Expression newOperand2) {
		return new Divide(newOperand1, newOperand2);
	}
}
