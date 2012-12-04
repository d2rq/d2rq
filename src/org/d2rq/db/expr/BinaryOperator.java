package org.d2rq.db.expr;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
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
	
	public Set<ColumnName> getColumns() {
		return columns;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
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
		return operator.hashCode() ^ expr1.hashCode() ^ expr2.hashCode();
	}
}
