package de.fuberlin.wiwiss.d2rq.expr;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;


public class Multiply extends BinaryOperator {

	public Multiply(Expression expr1, Expression expr2) {
		super(expr1, expr2, "*");
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new Multiply(expr1.renameAttributes(columnRenamer), expr2.renameAttributes(columnRenamer));
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
