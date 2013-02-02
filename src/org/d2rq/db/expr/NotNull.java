package org.d2rq.db.expr;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class NotNull extends UnaryExpression {

	public static Expression create(Expression expr) {
		return new NotNull(expr);
	}
	
	private NotNull(Expression operand) {
		super("NotNull", operand, GenericType.BOOLEAN);
	}

	@Override
	public Expression clone(Expression newOperand) {
		return NotNull.create(newOperand);
	}

	@Override
	public String toSQL(String operandSQL, Vendor vendor) {
		return operandSQL + " IS NOT NULL";
	}

	@Override
	public boolean isFalse() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return false;
	}

	@Override
	public boolean isTrue() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return false;
	}

	@Override
	public boolean isConstant() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return super.isConstant();
	}
	
	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return false;
	}
}
