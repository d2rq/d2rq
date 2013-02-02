package org.d2rq.db.expr;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.vendor.Vendor;


/**
 * An expression that negates an underlying expression
 * 
 * @author Christian Becker <http://beckr.org#chris>
 */
public class Negation extends UnaryExpression {
	
	public Negation(Expression operand) {
		super("Negation", operand, null);
	}

	@Override
	public Expression clone(Expression newOperand) {
		return new Negation(newOperand);
	}
	
	@Override
	public String toSQL(String operandSQL, Vendor vendor) {
		return "NOT (" + operandSQL + ")";
	}
	
	@Override
	public boolean isFalse() {
		return getOperand().isTrue();
	}

	@Override
	public boolean isTrue() {
		return getOperand().isFalse();
	}

	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		if (constIfTrue) {
			return getOperand().isConstantColumn(column, false, true, false);
		}
		if (constIfFalse) {
			return getOperand().isConstantColumn(column, true, false, false);
		}
		return super.isConstantColumn(column, false, false, true);
	}
}