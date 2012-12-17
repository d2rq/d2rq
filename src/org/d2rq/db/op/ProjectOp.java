package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Key;
import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;


/**
 * Also forces all projected columns and expression to be not null.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ProjectOp extends DatabaseOp.Wrapper {
	
	public static ProjectOp create(DatabaseOp wrapped, ColumnName... columns) {
		return new ProjectOp(ProjectionSpec.createFromColumns(columns), wrapped);
	}
	
	public static ProjectOp create(DatabaseOp wrapped, Collection<ProjectionSpec> specs) {
		return new ProjectOp(new ArrayList<ProjectionSpec>(specs), wrapped);
	}
	
	public static ProjectOp create(DatabaseOp wrapped, ProjectionSpec... specs) {
		return new ProjectOp(Arrays.asList(specs), wrapped);
	}
	
	public static DatabaseOp extend(DatabaseOp wrapped, 
			Map<ColumnName,Expression> extensions, Vendor vendor) {
		if (extensions.isEmpty()) return wrapped;
		List<ProjectionSpec> specs = ProjectionSpec.createFromColumns(
				wrapped.getColumns());
		for (ColumnName column: extensions.keySet()) {
			specs.add(ProjectionSpec.create(column, extensions.get(column), vendor));
		}
		return ProjectOp.create(wrapped, specs); 
	}
	
	private final List<ProjectionSpec> projections = new ArrayList<ProjectionSpec>();
	private final List<ColumnName> columnList = new ArrayList<ColumnName>();
	private final TreeMap<ColumnName,ProjectionSpec> columns =
		new TreeMap<ColumnName,ProjectionSpec>();
	private final Collection<Key> uniqueKeys =
		new ArrayList<Key>();
		
	private ProjectOp(List<ProjectionSpec> projections, DatabaseOp wrapped) {
		super(wrapped);
		for (ProjectionSpec spec: projections) {
			if (!spec.getColumn().isQualified()) {
				// See if there is a qualified version of this column in the wrapped
				for (ColumnName wrappedColumn: wrapped.getColumns()) {
					if (wrappedColumn.isQualified() && wrappedColumn.getColumn().equals(spec.getColumn().getColumn())) {
						spec = ProjectionSpec.create(wrappedColumn);
					}
				}
			}
			this.projections.add(spec);
			columnList.add(spec.getColumn());
		}
		// Build columns map
		for (ProjectionSpec spec: this.projections) {
			// Qualified names only
			if (!spec.getColumn().isQualified()) continue;
			if (columns.containsKey(spec.getColumn())) {
				throw new IllegalArgumentException("Duplicate column name " + spec.getColumn() + " in projection list: " + projections);
			}
			columns.put(spec.getColumn(), spec);
			if (columns.containsKey(spec.getColumn().getUnqualified())) {
				// Mark unqualified name as ambiguous
				columns.put(spec.getColumn().getUnqualified(), null);
			} else {
				columns.put(spec.getColumn().getUnqualified(), spec);
			}
		}
		for (ProjectionSpec spec: this.projections) {
			// Unqualified names only
			if (spec.getColumn().isQualified()) continue;
			if (columns.containsKey(spec.getColumn())) {
				throw new IllegalArgumentException("Duplicate column name " + spec.getColumn() + " in projection list: " + projections);
			}
			columns.put(spec.getColumn(), spec);
		}
		for (Key key: wrapped.getUniqueKeys()) {
			if (key.isContainedIn(columnList)) {
				uniqueKeys.add(key);
			}
		}
	}
	
	public List<ProjectionSpec> getProjections() {
		return projections;
	}
	
	public boolean hasColumn(ColumnName column) {
		if (columns.get(column) == null) {
			return false;	// Ambiguous unqualified name -- pretend it's not there
		}
		return columns.containsKey(column);
	}
	
	public List<ColumnName> getColumns() {
		return columnList;
	}

	public boolean isNullable(ColumnName column) {
		return false;
	}

	public DataType getColumnType(ColumnName column) {
		if (!hasColumn(column)) return null;
		return columns.get(column).getDataType(getWrapped());
	}

	public Collection<Key> getUniqueKeys() {
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
		return "Project(" + getWrapped() + "," + projections + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ projections.hashCode() ^ 70;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ProjectOp)) return false;
		ProjectOp other = (ProjectOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return projections.equals(other.projections);
	}
}
