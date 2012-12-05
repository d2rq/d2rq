package org.d2rq.db.expr;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;



/**
 * An {@link Expression} defined directly by a raw SQL string.
 * 
 * The SQL may be just a constant expression, or it may be an alternating
 * list of literal parts an column parts, in which case the actual SQL
 * expression is formed by concatenating all parts, renaming the
 * columns if necessary.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLExpression extends Expression {

	/**
	 * Only usable for expressions that don't mention any columns
	 */
	public static Expression create(String sql, GenericType dataType) {
		sql = sql.trim();
		if ("1".equals(sql)) {
			return Expression.TRUE;
		}
		if ("0".equals(sql)) {
			return Expression.FALSE;
		}
		return SQLExpression.create(Collections.singletonList(sql), 
				null, dataType);
	}
	
	/**
	 * Creates a SQL expression that may contain column references.
	 * The actual SQL expression is obtained by alternating a literal
	 * part with a column part. There must be one more literal parts
	 * than column parts.
	 */
	public static SQLExpression create(List<String> literalParts, 
			List<ColumnName> columns, GenericType dataType) {
		if (columns == null) columns = Collections.emptyList();
		if (literalParts.size() != columns.size() + 1) {
			throw new IllegalArgumentException("Must have one more literal part than column parts");
		}
		return new SQLExpression(literalParts, columns, dataType);
	}
	
	private final List<String> literalParts;
	private final List<ColumnName> columns;
	private final Set<ColumnName> columnsAsSet;
	private final GenericType dataType;
	private final String asString;
	
	private SQLExpression(List<String> literalParts, List<ColumnName> columns, 
			GenericType dataType) {
		this.literalParts = literalParts;
		this.columns = columns;
		this.columnsAsSet = new HashSet<ColumnName>(columns);
		this.dataType = dataType;
		this.asString = "SQL" + toSQL(null, Vendor.SQL92);
	}

	public boolean isTrue() {
		return false;
	}
	
	public boolean isFalse() {
		return false;
	}
	
	public Set<ColumnName> getColumns() {
		return this.columnsAsSet;
	}
	
	public Expression rename(Renamer renamer) {
		return new SQLExpression(literalParts, renamer.applyToColumns(columns), dataType);
	}
	
	public String toSQL(DatabaseOp table, Vendor vendor) {
		StringBuffer result = new StringBuffer("(");
		for (int i = 0; i < columns.size(); i++) {
			result.append(literalParts.get(i));
			result.append(vendor.toString(columns.get(i)));
		}
		result.append(literalParts.get(literalParts.size() - 1));
		result.append(")");
		return result.toString();
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return dataType.dataTypeFor(vendor);
	}
	
	public String toString() {
		return asString;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof SQLExpression)) {
			return false;
		}
		SQLExpression otherExpression = (SQLExpression) other;
		return literalParts.equals(otherExpression.literalParts) && 
				columns.equals(otherExpression.columns) && 
				dataType.equals(otherExpression.dataType);
	}
	
	public int hashCode() {
		return literalParts.hashCode() ^ columns.hashCode() ^ dataType.hashCode() ^ 6543;
	}
}
