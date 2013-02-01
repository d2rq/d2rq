package org.d2rq.db.expr;

import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class NotNull extends Expression {

	public static Expression create(Expression expr) {
		return new NotNull(expr);
	}
	
	private Expression expr;
	
	private NotNull(Expression expr) {
		this.expr = expr;
	}
	
	public Set<ColumnName> getColumns() {
		return expr.getColumns();
	}

	public boolean isFalse() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return false;
	}

	public boolean isTrue() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return false;
	}

	public boolean isConstant() {
		// TODO: If we knew the DatabaseOp here, we could answer precisely
		return expr.isConstant();
	}
	
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		return false;
	}

	public Expression rename(Renamer columnRenamer) {
		return NotNull.create(columnRenamer.applyTo(expr));
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return expr.toSQL(table, vendor) + " IS NOT NULL";
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.BOOLEAN.dataTypeFor(vendor);
	}
	
	public String toString() {
		return "NotNull(" + this.expr + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof NotNull)) {
			return false;
		}
		NotNull otherExpression = (NotNull) other;
		return expr.equals(otherExpression.expr); 
	}
	
	public int hashCode() {
		return this.expr.hashCode() ^ 58473;
	}
}
