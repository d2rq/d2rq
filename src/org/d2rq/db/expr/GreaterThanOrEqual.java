package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class GreaterThanOrEqual extends BinaryOperator {

	public GreaterThanOrEqual(Expression expr1, Expression expr2) {
		super(expr1, expr2, ">=", false, GenericType.BOOLEAN);
	}

	public Expression rename(Renamer columnRenamer) {
		return new GreaterThanOrEqual(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}

}
