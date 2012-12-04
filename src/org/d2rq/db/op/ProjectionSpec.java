package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


/**
 * Something to be used in the SELECT clause of a SQL query, e.g.
 * a column name or an expression.
 *
 * TODO: ProjectionSpec is kind of conceptually broken. {@link ColumnName} should be used instead. Instead of creating {@link ExprProjectionSpec} instances, modify the {@link DatabaseOp} by adding a {@link ProjectOp}
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class ProjectionSpec implements Comparable<ProjectionSpec> {

	public abstract ColumnName getColumn();

	public abstract Set<TableName> getTableNames();
	
	public abstract ProjectionSpec rename(Renamer renamer);
	
	public abstract String toSQL(DatabaseOp table, Vendor vendor);
	
	public abstract DataType getDataType(DatabaseOp table);
	
	public static ProjectionSpec create(ColumnName column) {
		return new ColumnProjectionSpec(column);
	}

	public static List<ProjectionSpec> createFromColumns(ColumnName[] columns) {
		return createFromColumns(Arrays.asList(columns));
	}

	public static List<ProjectionSpec> createFromColumns(List<ColumnName> columns) {
		List<ProjectionSpec> result = new ArrayList<ProjectionSpec>();
		for (ColumnName column: columns) {
			result.add(new ColumnProjectionSpec(column));
		}
		return result;
	}

	/**
	 * Creates a projection spec from an expression.
	 * @param expression An expression
	 * @param vendor For determining the expression's datatype
	 * @return A matching projection spec
	 */
	public static ProjectionSpec create(Expression expression, Vendor vendor) {
		return new ExprProjectionSpec(expression, vendor);
	}

	public static class ColumnProjectionSpec extends ProjectionSpec {
		private final ColumnName column;
		private ColumnProjectionSpec(ColumnName column) {
			this.column = column;
		}
		public ColumnName getColumn() {
			return column;
		}
		public Set<TableName> getTableNames() {
			return column.isQualified()
					? Collections.singleton(column.getQualifier()) 
					: Collections.<TableName>emptySet();
		}
		public ProjectionSpec rename(Renamer renamer) {
			return new ColumnProjectionSpec(renamer.applyTo(column));
		}
		public String toSQL(DatabaseOp table, Vendor vendor) {
			return vendor.toString(column);
		}
		public DataType getDataType(DatabaseOp table) {
			return table.getColumnType(column);
		}
		@Override
		public String toString() {
			return column.toString();
		}
		@Override
		public int hashCode() {
			return column.hashCode() ^ 445;
		}
		@Override 
		public boolean equals(Object o) {
			if (!(o instanceof ColumnProjectionSpec)) return false;
			return ((ColumnProjectionSpec) o).column.equals(column);
		}
		public int compareTo(ProjectionSpec o) {
			if (!(o instanceof ColumnProjectionSpec)) {
				return -1;
			}
			ColumnProjectionSpec other = (ColumnProjectionSpec) o;
			return column.compareTo(other.column);
		}
	}
	
	public static class ExprProjectionSpec extends ProjectionSpec {
		private final Expression expression;
		private final Vendor vendor;
		private final ColumnName name;
		private final Set<TableName> tables = new HashSet<TableName>();
		private ExprProjectionSpec(Expression expression, Vendor vendor) {
			this.expression = expression;
			this.vendor = vendor;
			this.name = ColumnName.create(Identifier.createUndelimited(
					"EXPR" + Integer.toHexString(expression.hashCode())));
			for (ColumnName column: expression.getColumns()) {
				if (column.isQualified()) {
					tables.add(column.getQualifier());
				}
			}
		}
		public Expression getExpression() {
			return expression;
		}
		public ProjectionSpec rename(Renamer renamer) {
			return new ExprProjectionSpec(renamer.applyTo(expression), vendor);
		}
		public ColumnName getColumn() {
			return name;
		}
		public Set<TableName> getTableNames() {
			return tables;
		}
		public String toSQL(DatabaseOp table, Vendor vendor) {
			return expression.toSQL(table, vendor) + vendor.getAliasOperator() + vendor.toString(name);
		}
		public DataType getDataType(DatabaseOp table) {
			return expression.getDataType(table, vendor);
		}
		public boolean equals(Object other) {
			return (other instanceof ExprProjectionSpec) 
					&& expression.equals(((ExprProjectionSpec) other).expression);
		}
		public int hashCode() {
			return expression.hashCode() ^ 684036;
		}
		public String toString() {
			return expression + " AS " + name;
		}
		public int compareTo(ProjectionSpec other) {
			if (!(other instanceof ExprProjectionSpec)) {
				return 1;
			}
			ExprProjectionSpec otherExpr = (ExprProjectionSpec) other;
			return name.compareTo(otherExpr.name);
		}
	}
}
