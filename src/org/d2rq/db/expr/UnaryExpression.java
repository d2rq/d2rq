package org.d2rq.db.expr;

import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public abstract class UnaryExpression extends Expression {
	private final String name;
	private final Expression operand;
	private final GenericType dataType;
	
	public UnaryExpression(String name, Expression base, GenericType dataType) {
		this.name = name;
		this.operand = base;
		this.dataType = dataType;
	}

	public Expression getOperand() {
		return operand;
	}

	public abstract Expression clone(Expression newOperand);
	
	public abstract String toSQL(String baseSQL, Vendor vendor);
	
	@Override
	public Set<ColumnName> getColumns() {
		return operand.getColumns();
	}

	@Override
	public boolean isFalse() {
		return operand.isFalse();
	}

	@Override
	public boolean isTrue() {
		return operand.isTrue();
	}

	@Override
	public boolean isConstant() {
		return operand.isConstant();
	}

	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		return operand.isConstantColumn(column, constIfTrue, constIfFalse, constIfConstantValue);
	}

	@Override
	public Expression rename(Renamer renamer) {
		Expression renamed = operand.rename(renamer);
		return operand.equals(renamed) ? this : clone(operand.rename(renamer));
	}

	@Override
	public Expression substitute(ColumnName column, Expression substitution) {
		Expression substituted = operand.substitute(column, substitution);
		return operand.equals(substituted) ? this : clone(substituted);
	}
	
	@Override
	public String toSQL(DatabaseOp table, Vendor vendor) {
		return toSQL(operand.toSQL(table, vendor), vendor);
	}
	
	/**
	 * Returns the data type provided in the constructor, or the data type
	 * of the operand if <code>null</code> was provided there.
	 */
	@Override
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return dataType == null ? 
				operand.getDataType(table, vendor) : dataType.dataTypeFor(vendor);
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(name);
		result.append('(');
		result.append(operand);
		result.append(')');
		return result.toString();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() ^ operand.hashCode() ^ 469; 
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UnaryExpression)) return false;
		UnaryExpression other = (UnaryExpression) o;
		return other.name.equals(this.name) && other.operand.equals(this.operand);
	}
}