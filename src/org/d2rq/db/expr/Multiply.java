package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class Multiply extends BinaryOperator {

	public Multiply(Expression expr1, Expression expr2) {
		super(expr1, expr2, "*", true, GenericType.NUMERIC);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Multiply(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}

	public boolean equals(Object other) {
		if (!(other instanceof Multiply)) {
			return false;
		}
		Multiply otherMultiply = (Multiply) other;
		if (expr1.equals(otherMultiply.expr1) && expr2.equals(otherMultiply.expr2)) {
			return true;
		}
		if (expr1.equals(otherMultiply.expr2) && expr2.equals(otherMultiply.expr1)) {
			return true;
		}
		return false;
	}


}
