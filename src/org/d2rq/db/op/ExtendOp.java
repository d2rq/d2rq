package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


/**
 * Adds a new column computed from an expression.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ExtendOp extends DatabaseOp.Wrapper {
	
	public static DatabaseOp extend(DatabaseOp wrapped, Identifier newColumn,
			Expression expression, Vendor vendor) {
		return new ExtendOp(wrapped, newColumn, expression, vendor);
	}
	
	public static DatabaseOp extend(DatabaseOp wrapped, 
			Map<Identifier,Expression> extensions, Vendor vendor) {
		DatabaseOp result = wrapped;
		for (Identifier column: extensions.keySet()) {
			result = new ExtendOp(result, column, extensions.get(column), vendor);
		}
		return result;
	}
	
	/**
	 * Returns a column name guaranteed to be unique to the given expression.
	 * Subsequent invocations with the same expression return the same name.
	 */
	public static Identifier createUniqueIdentifierFor(Expression expression) {
		return Identifier.createUndelimited(
				"EXPR_" + Integer.toHexString(expression.hashCode()).toUpperCase());
	}

	private final Identifier newColumn;
	private final Expression expression;
	private final ColumnList allColumns;
	private final Vendor vendor;
	
	private ExtendOp(DatabaseOp wrapped, Identifier newColumn, 
			Expression expression, Vendor vendor) {
		super(wrapped);
		this.newColumn = newColumn;
		this.expression = expression;
		this.vendor = vendor;
		List<ColumnName> columns = new ArrayList<ColumnName>();
		for (ColumnName col: wrapped.getColumns()) {
			columns.add(col);
		}
		columns.add(ColumnName.create(newColumn));
		this.allColumns = ColumnList.create(columns);
	}
	
	public Identifier getNewColumn() {
		return newColumn;
	}
	
	public Expression getExpression() {
		return expression;
	}
	
	public Vendor getVendor() {
		return vendor;
	}
	
	public boolean hasColumn(ColumnName column) {
		if (allColumns.isAmbiguous(column)) return false;
		return allColumns.contains(column);
	}
	
	public ColumnList getColumns() {
		return allColumns;
	}

	public boolean isNullable(ColumnName column) {
		if (allColumns.isAmbiguous(column)) return false;
		if (!column.isQualified() && column.getColumn().equals(newColumn)) {
			return true;	// As far as we know, expression might be nullable 
		}
		return getWrapped().isNullable(column);
	}

	public DataType getColumnType(ColumnName column) {
		if (allColumns.isAmbiguous(column)) return null;
		if (!column.isQualified() && column.getColumn().equals(newColumn)) {
			return expression.getDataType(getWrapped(), vendor); 
		}
		return getWrapped().getColumnType(column);
	}

	public Collection<ColumnList> getUniqueKeys() {
		return getWrapped().getUniqueKeys();
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Extend(" + newColumn + "<=" + expression + ", " + getWrapped() + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ newColumn.hashCode() ^ expression.hashCode() ^ 70;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExtendOp)) return false;
		ExtendOp other = (ExtendOp) o;
		if (!newColumn.equals(other.newColumn)) return false;
		if (!expression.equals(other.expression)) return false;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return true;
	}
}
