package org.d2rq.db.expr;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;


/**
 * An expression that is TRUE iff its two constituent expressions are true.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Equality extends BinaryOperator {

	public static Expression create(Expression expr1, Expression expr2) {
		if (expr1.equals(expr2)) {
			return Expression.TRUE;
		}
		return new Equality(expr1, expr2);
	}
	
	public static Expression createColumnEquality(
			ColumnName column1, ColumnName column2) {
		return (column1.compareTo(column2) < 0)
				? create(new ColumnExpr(column1), new ColumnExpr(column2))
				: create(new ColumnExpr(column2), new ColumnExpr(column1));
	}

	public static Expression createColumnValue(
			ColumnName column, String value, DataType dataType) {
		return create(new ColumnExpr(column), Constant.create(value, dataType));
	}
	
	public static Expression createExpressionValue(
			Expression expression, String value, DataType dataType) {
		return create(expression, Constant.create(value, dataType));
	}
	
	private Equality(Expression expr1, Expression expr2) {
		super(expr1, expr2, "=", true, GenericType.BOOLEAN);
	}

	@Override
	protected Expression clone(Expression newOperand1, Expression newOperand2) {
		return new Equality(newOperand1, newOperand2);
	}

	@Override
	public boolean isFalse() {
		return (expr1.isFalse() && expr2.isTrue())
				|| (expr1.isTrue() && expr2.isFalse());
	}

	@Override
	public boolean isTrue() {
		return expr1.equals(expr2);
	}

	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfTrue) return false;
		if (expr1.isConstant()) {
			return (expr2.isConstantColumn(column, false, false, true));
		}
		if (expr2.isConstant()) {
			return (expr1.isConstantColumn(column, false, false, true));
		}
		return false;
	}
}
