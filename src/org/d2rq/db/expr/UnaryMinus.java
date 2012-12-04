package org.d2rq.db.expr;

import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class UnaryMinus extends Expression {

	private Expression base;
	
	public UnaryMinus(Expression base) {
		this.base = base;
	}
	
	public Expression getBase() {
		return base;
	}

	public Set<ColumnName> getColumns() {
		return base.getColumns();
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression rename(Renamer columnRenamer) {
		return new UnaryMinus(base.rename(columnRenamer));
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return "- (" + base.toSQL(table, vendor) + ")";
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.NUMERIC.dataTypeFor(vendor);
	}
	
	public String toString() {
		return "- (" + base + ")";
	}

}
