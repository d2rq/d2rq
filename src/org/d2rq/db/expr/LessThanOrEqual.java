package org.d2rq.db.expr;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;



public class LessThanOrEqual extends BinaryOperator {

	public LessThanOrEqual(Expression expr1, Expression expr2) {
		super(expr1, expr2, "<=", false, GenericType.BOOLEAN);
	}

	public Expression rename(Renamer columnRenamer) {
		return new LessThanOrEqual(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}

}
