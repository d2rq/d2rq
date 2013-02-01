package org.d2rq.db.expr;

import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


/**
 * An expression that negates an underlying expression
 * 
 * @author Christian Becker <http://beckr.org#chris>
 */
public class Negation extends Expression {
	
	private Expression base;
	
	public Negation(Expression base) {
		this.base = base;
	}

	public Expression getBase() {
		return base;
	}
	
	public Set<ColumnName> getColumns() {
		return base.getColumns();
	}

	public boolean isFalse() {
		return base.isTrue();
	}

	public boolean isTrue() {
		return base.isFalse();
	}

	public boolean isConstant() {
		return base.isConstant();
	}
	
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		if (constIfTrue) {
			return base.isConstantColumn(column, false, true, false);
		}
		if (constIfFalse) {
			return base.isConstantColumn(column, true, false, false);
		}
		return base.isConstantColumn(column, false, false, true);
	}

	public Expression rename(Renamer columnRenamer) {
		return new Negation(base.rename(columnRenamer));
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return "NOT (" + base.toSQL(table, vendor) + ")";
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return base.getDataType(table, vendor);
	}
	
	public String toString() {
		return "Negation(" + base + ")";
	}
}