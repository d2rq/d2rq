package de.fuberlin.wiwiss.d2rq.expr;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;


public class GreaterThan extends BinaryOperator {

	public GreaterThan(Expression expr1, Expression expr2) {
		super(expr1, expr2, ">");
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new GreaterThan(expr1.renameAttributes(columnRenamer), expr2.renameAttributes(columnRenamer));
	}

}
