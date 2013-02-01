package org.d2rq.db.expr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;



/**
 * A SQL expression.
 * 
 * TODO: Should introduce an ExpressionVisitor and replace most methods with implementations of that
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class Expression {
	public static final Expression TRUE = new Expression() {
		public Set<ColumnName> getColumns() { return Collections.<ColumnName>emptySet(); }
		public boolean isFalse() { return false; }
		public boolean isTrue() { return true; }
		public boolean isConstant() { return true; }
		public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
				boolean constIfFalse, boolean constIfConstantValue) { return false; }
		public Expression rename(Renamer columnRenamer) { return this; }
		public DataType getDataType(DatabaseOp table, Vendor vendor) { return GenericType.BOOLEAN.dataTypeFor(vendor); }
		public String toSQL(DatabaseOp table, Vendor vendor) { return "1"; }
		public String toString() { return "TRUE"; }
	};
	public static final Expression FALSE = new Expression() {
		public Set<ColumnName> getColumns() { return Collections.<ColumnName>emptySet(); }
		public boolean isFalse() { return true; }
		public boolean isTrue() { return false; }
		public boolean isConstant() { return true; }
		public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
				boolean constIfFalse, boolean constIfConstantValue) { return false; }
		public Expression rename(Renamer columnRenamer) { return this; }
		public DataType getDataType(DatabaseOp table, Vendor vendor) { return GenericType.BOOLEAN.dataTypeFor(vendor); }
		public String toSQL(DatabaseOp table, Vendor vendor) { return "0"; }
		public String toString() { return "FALSE"; }
	};
	
	public abstract boolean isTrue();
	
	public abstract boolean isFalse();
	
	public abstract boolean isConstant();
	
	/**
	 * Checks whether this expression forces a given column to be a constant,
	 * that is, have the same value everywhere. For example,
	 * T.COL1=5 forces T.COL1 to be constant if the expression is assumed
	 * to be true; T.COL1+1 forces it to be constant if the expression
	 * is assumed to evaluate to a constant value. Exactly one of the three
	 * constXXX arguments must be true, the other two must be false.
	 *
	 * @param column The column to be checked
	 * @param constIfTrue Is the column constant assuming the expression evaluates to true?
	 * @param constIfFalse Is the column constant assuming the expression evaluates to false?
	 * @param constIfConstantValue Is column constant assuming the expression evaluates to a value equal to some constant?
	 */
	public abstract boolean isConstantColumn(ColumnName column, 
			boolean constIfTrue, boolean constIfFalse, boolean constIfConstantValue);
	
	public abstract Set<ColumnName> getColumns();
	
	public abstract Expression rename(Renamer columnRenamer);
	
	public abstract DataType getDataType(DatabaseOp table, Vendor vendor);

	public abstract String toSQL(DatabaseOp table, Vendor vendor);

	public Expression and(Expression other) {
		List<Expression> list = new ArrayList<Expression>(2);
		list.add(this);
		list.add(other);
		return Conjunction.create(list);
	}

	public Expression or(Expression other) {
		List<Expression> list = new ArrayList<Expression>(2);
		list.add(this);
		list.add(other);
		return Disjunction.create(list);
	}
}
