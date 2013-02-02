package org.d2rq.db.expr;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;



public abstract class BinaryOperator extends Expression {
	protected final Expression expr1;
	protected final Expression expr2;
	protected final String operator;
	private final boolean isCommutative;
	private final GenericType dataType;
	private final Set<ColumnName> columns = new HashSet<ColumnName>();
	
	protected BinaryOperator(Expression expr1, Expression expr2, 
			String operator, boolean isCommutative, GenericType dataType) {
		this.expr1 = expr1;
		this.expr2 = expr2;
		this.operator = operator;
		this.isCommutative = isCommutative;
		this.dataType = dataType;
		columns.addAll(expr1.getColumns());
		columns.addAll(expr2.getColumns());
	}
	
	protected abstract Expression clone(Expression newOperand1, Expression newOperand2);
	
	public Set<ColumnName> getColumns() {
		return columns;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public boolean isConstant() {
		return expr1.isConstant() && expr2.isConstant();
	}

	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		if (!constIfConstantValue) return false;
		if (expr1.isConstant()) {
			return expr2.isConstantColumn(column, false, false, true);
		}
		if (expr2.isConstant()) {
			return expr1.isConstantColumn(column, false, false, true);
		}
		return false;
	}

	@Override
	public Expression rename(Renamer columnRenamer) {
		return clone(expr1.rename(columnRenamer), expr2.rename(columnRenamer));
	}
	
	public Expression substitute(ColumnName column, Expression substitution) {
		return clone(expr1.substitute(column, substitution), expr2.substitute(column, substitution));
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return dataType.dataTypeFor(vendor);
	}
	
	public String toSQL(DatabaseOp table, Vendor vendor) {
		return expr1.toSQL(table, vendor) + operator + expr2.toSQL(table, vendor);
	}
	
	public String toString() {
		return operator + "(" + expr1 + ", " + expr2 + ")";
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof BinaryOperator)) {
			return false;
		}
		BinaryOperator other = (BinaryOperator) o;
		if (!operator.equals(other.operator)) return false;
		if (expr1.equals(other.expr1) && expr2.equals(other.expr2)) {
			return true;
		}
		if (isCommutative && other.isCommutative && expr1.equals(other.expr2) && expr2.equals(other.expr1)) {
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		return operator.hashCode() ^ expr1.hashCode() ^ (expr2.hashCode() + (isCommutative ? 0 : 1));
	}
}
