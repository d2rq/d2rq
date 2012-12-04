package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class Divide extends BinaryOperator {

	public Divide(Expression expr1, Expression expr2) {
		super(expr1, expr2, "/", false, GenericType.NUMERIC);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Divide(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}

}
