package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class Subtract extends BinaryOperator {

	public Subtract(Expression expr1, Expression expr2) {
		super(expr1, expr2, "-", false, GenericType.NUMERIC);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Subtract(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}
}
