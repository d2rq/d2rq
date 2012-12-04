package org.d2rq.db.expr;

import java.util.Collections;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


public class ColumnExpr extends Expression {
	private final ColumnName column;
	
	public ColumnExpr(ColumnName column) {
		this.column = column;
	}
	
	public Set<ColumnName> getColumns() {
		return Collections.singleton(column);
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression rename(Renamer columnRenamer) {
		return new ColumnExpr(columnRenamer.applyTo(column));
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return vendor.toString(column);
	}

	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return table.getColumnType(column);
	}
	
	public String toString() {
		return "ColumnExpr(" + column + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof ColumnExpr)) {
			return false;
		}
		return column.equals(((ColumnExpr) other).column);
	}
	
	public int hashCode() {
		return column.hashCode();
	}
}
