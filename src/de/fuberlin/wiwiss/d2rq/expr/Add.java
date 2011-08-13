package de.fuberlin.wiwiss.d2rq.expr;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;


public class Add extends BinaryOperator {

	public Add(Expression expr1, Expression expr2) {
		super(expr1, expr2, "+");
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new Add(expr1.renameAttributes(columnRenamer), expr2.renameAttributes(columnRenamer));
	}


	public boolean equals(Object other) {
		if (!(other instanceof Add)) {
			return false;
		}
		Add otherAdd = (Add) other;
		if (expr1.equals(otherAdd.expr1) && expr2.equals(otherAdd.expr2)) {
			return true;
		}
		if (expr1.equals(otherAdd.expr2) && expr2.equals(otherAdd.expr1)) {
			return true;
		}
		return false;
	}


}
