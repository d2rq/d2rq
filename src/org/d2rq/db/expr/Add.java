package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class Add extends BinaryOperator {

	public Add(Expression expr1, Expression expr2) {
		super(expr1, expr2, "+", true, GenericType.NUMERIC);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Add(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
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
