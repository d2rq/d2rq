package org.d2rq.db.expr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;



public abstract class NAryExpression extends Expression {
	private final Expression[] operands;
	private final String name;
	private final GenericType dataType;
	private final Set<ColumnName> columns = new HashSet<ColumnName>();
	
	protected NAryExpression(String name, Expression[] parts, 
			boolean isCommutative, GenericType dataType) {
		if (isCommutative) {
			this.operands = Arrays.copyOf(parts, parts.length);
			Arrays.sort(this.operands);
		} else {
			this.operands = parts;
		}
		this.name = name;
		this.dataType = dataType;
		for (Expression expression: parts) {
			columns.addAll(expression.getColumns());
		}
	}
	
	protected abstract Expression clone(Expression[] newParts);
	
	protected abstract String toSQL(String[] sqlFragments, Vendor vendor);
	
	public Expression[] getOperands() {
		return operands;
	}
	
	@Override
	public Set<ColumnName> getColumns() {
		return columns;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isConstant() {
		for (Expression expression: operands) {
			if (!expression.isConstant()) return false;
		}
		return true;
	}

	@Override
	public Expression rename(Renamer columnRenamer) {
		Expression[] renamedExpressions = new Expression[operands.length];
		for (int i = 0; i < operands.length; i++) {
			renamedExpressions[i] = columnRenamer.applyTo(operands[i]);
		}
		return clone(renamedExpressions);
	}
	
	@Override
	public Expression substitute(ColumnName column, Expression substitution) {
		Expression[] substituted = new Expression[operands.length];
		for (int i = 0; i < operands.length; i++) {
			substituted[i] = operands[i].substitute(column, substitution);
		}
		return clone(substituted);
	}

	@Override
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return dataType.dataTypeFor(vendor);
	}
	
	@Override
	public String toSQL(DatabaseOp table, Vendor vendor) {
		String[] fragments = new String[operands.length];
		for (int i = 0; i < operands.length; i++) {
			Expression part = (Expression) operands[i];
			fragments[i] = part.toSQL(table, vendor);
		}
		return toSQL(fragments, vendor);
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(name);
		result.append("(");
		for (int i = 0; i < operands.length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(operands[i]);
		}
		result.append(")");
		return result.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NAryExpression)) return false;
		NAryExpression o = (NAryExpression) other;
		return name.equals(o.name) && Arrays.equals(operands, o.operands);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(operands) ^ name.hashCode() ^ 234645;
	}
}
