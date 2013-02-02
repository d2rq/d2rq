package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;


/**
 * Retains a subset of columns, discarding the rest.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ProjectOp extends DatabaseOp.Wrapper {
	
	public static ProjectOp project(DatabaseOp wrapped, ColumnName... columns) {
		return project(wrapped, Arrays.asList(columns));
	}
	
	public static ProjectOp project(DatabaseOp wrapped, List<ColumnName> columns) {
		return new ProjectOp(columns, wrapped);
	}
	
	public static ProjectOp project(DatabaseOp wrapped, ColumnList columns) {
		return new ProjectOp(columns.asList(), wrapped);
	}
	
	private final ColumnList columns;
	private final Collection<ColumnList> uniqueKeys = new ArrayList<ColumnList>();
	
	private ProjectOp(List<ColumnName> projections, DatabaseOp wrapped) {
		super(wrapped);
		List<ColumnName> columns = new ArrayList<ColumnName>();
		for (ColumnName column: projections) {
			// See if there is a qualified version of this column in the wrapped
			ColumnName wrappedColumn = wrapped.getColumns().get(column);
			columns.add(wrappedColumn == null ? column : wrappedColumn);
		}
		this.columns = ColumnList.create(columns);
		for (ColumnList key: wrapped.getUniqueKeys()) {
			if (this.columns.containsAll(key)) {
				uniqueKeys.add(key);
			}
		}
	}
	
	public boolean hasColumn(ColumnName column) {
		if (columns.isAmbiguous(column)) return false;
		return columns.contains(column);
	}
	
	public ColumnList getColumns() {
		return columns;
	}

	public boolean isNullable(ColumnName column) {
		return columns.contains(column) && getWrapped().isNullable(column);
	}

	public DataType getColumnType(ColumnName column) {
		if (!columns.contains(column)) return null;
		return getWrapped().getColumnType(column);
	}

	public Collection<ColumnList> getUniqueKeys() {
		return uniqueKeys;
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Project(" + columns + ", " + getWrapped() + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ columns.hashCode() ^ 70;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ProjectOp)) return false;
		ProjectOp other = (ProjectOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return columns.equals(other.columns);
	}
}
