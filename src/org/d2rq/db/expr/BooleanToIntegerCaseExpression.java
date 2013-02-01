package org.d2rq.db.expr;

import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


/**
 * A CASE statement that turns a BOOLEAN (TRUE, FALSE) into an
 * INT (1, 0)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BooleanToIntegerCaseExpression extends Expression {
	private Expression base;
	
	public BooleanToIntegerCaseExpression(Expression base) {
		this.base = base;
	}

	public Expression getBase() {
		return base;
	}
	
	public Set<ColumnName> getColumns() {
		return base.getColumns();
	}

	public boolean isFalse() {
		return base.isFalse();
	}

	public boolean isTrue() {
		return base.isTrue();
	}

	public boolean isConstant() {
		return base.isConstant();
	}

	public boolean isConstantColumn(ColumnName column, boolean constIfTrue,
			boolean constIfFalse, boolean constIfConstantValue) {
		return base.isConstantColumn(column, constIfTrue, constIfFalse, constIfConstantValue);
	}

	public Expression rename(Renamer columnRenamer) {
		return new BooleanToIntegerCaseExpression(base.rename(columnRenamer));
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return "(CASE WHEN (" + base.toSQL(table, vendor) + ") THEN 1 ELSE 0 END)";
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.NUMERIC.dataTypeFor(vendor);
	}
	
	public String toString() {
		return "Boolean2Int(" + base + ")";
	}

	public boolean equals(Object other) {
		if (!(other instanceof BooleanToIntegerCaseExpression)) {
			return false;
		}
		BooleanToIntegerCaseExpression otherExpression = (BooleanToIntegerCaseExpression) other;
		return this.base.equals(otherExpression.base); 
	}
	
	public int hashCode() {
		return base.hashCode() ^ 2341234;
	}
}