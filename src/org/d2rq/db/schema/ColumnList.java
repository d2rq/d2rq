package org.d2rq.db.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A list of possibly qualified {@link ColumnName}s that can be accessed
 * via the qualified or unqualified versions of the names.  
 */
public class ColumnList implements Iterable<ColumnName> {
	
	public final static ColumnList EMPTY = 
			new ColumnList(Collections.<ColumnName>emptyList());

	public static ColumnList create(List<ColumnName> columns) {
		if (columns == null) return null;
		return new ColumnList(columns);
	}
	
	public static ColumnList create(ColumnName... columns) {
		if (columns == null) return null;
		return new ColumnList(Arrays.asList(columns));
	}
	
	public static ColumnList create(TableName table, IdentifierList columns) {
		if (columns == null) return null;
		List<ColumnName> qualified = new ArrayList<ColumnName>();
		for (Identifier col: columns) {
			qualified.add(table.qualifyIdentifier(col));
		}
		return new ColumnList(qualified);
	}
	
	private final List<ColumnName> columns;
	
	private ColumnList(List<ColumnName> columns) {
		this.columns = new ArrayList<ColumnName>(columns);
	}
	
	public Iterator<ColumnName> iterator() {
		return columns.iterator();
	}
	
	public int size() {
		return columns.size();
	}
	
	public boolean isEmpty() {
		return columns.isEmpty();
	}
	
	public ColumnName get(int index) {
		return columns.get(index);
	}

	public int indexOf(ColumnName column) {
		if (isAmbiguous(column)) return -1;
		for (int i = 0; i < size(); i++) {
			if (column.isQualified()) {
				if (column.equals(get(i))) return i;
			} else {
				if (column.getColumn().equals(get(i).getColumn())) return i;
			}
		}
		return -1;
	}
	
	/**
	 * Checks whether an (unqualified) identifier is used in multiple
	 * qualified column names in this list
	 * @return
	 */
	public boolean isAmbiguous(Identifier identifier) {
		boolean foundOnce = false;
		for (ColumnName column: columns) {
			if (!column.getColumn().equals(identifier)) continue;
			if (foundOnce) return true;
			foundOnce = true;
		}
		return false;
	}
	
	/**
	 * Checks whether an unqualified column name is used in multiple
	 * qualified column names in this list. Qualified column names are
	 * considered unambiguous.
	 */
	public boolean isAmbiguous(ColumnName column) {
		if (column.isQualified()) return false;
		return isAmbiguous(column.getColumn());
	}
	
	/**
	 * @param candidate A qualified or unqualified version of a column name
	 * @return False if the candidate is not in the list, or is ambiguous
	 */
	public boolean contains(ColumnName candidate) {
		if (candidate.isQualified()) return columns.contains(candidate);
		return contains(candidate.getColumn());
	}
	
	/**
	 * Checks if an identifier is used in the column list (qualified or unqualified)
	 * @return False if the candidate is not in the list, or is ambiguous
	 */
	public boolean contains(Identifier candidate) {
		for (ColumnName column: columns) {
			if (candidate.equals(column.getColumn())) return true;
		}
		return false;
	}
	
	/**
	 * True if all columns in the other list are contained in this one. Does
	 * not verify that all references are unambiguous or that the presence
	 * of qualifiers is consistent.
	 */
	public boolean containsAll(ColumnList other) {
		for (ColumnName column: other) {
			if (!contains(column)) return false;
		}
		return true;
	}
	
	/**
	 * Returns the qualified version of the column name if possible. Returns
	 * null for ambiguous or non-present columns.
	 */
	public ColumnName get(ColumnName candidate) {
		if (candidate.isQualified()) {
			if (columns.contains(candidate)) return candidate;
			return null;
		}
		return get(candidate.getColumn());
	}
	
	/**
	 * Returns the qualified version of the identifier if possible. Returns
	 * null for ambiguous or non-present columns.
	 */
	public ColumnName get(Identifier candidate) {
		if (isAmbiguous(candidate)) return null;
		for (ColumnName column: columns) {
			if (candidate.equals(column.getColumn())) return column;
		}
		return null;
	}
	
	public List<ColumnName> asList() {
		return columns;
	}
	
	@Override
	public String toString() {
		return columns.toString();
	}
	
	@Override
	public int hashCode() {
		return columns.hashCode() ^ 342;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ColumnList)) return false;
		return ((ColumnList) o).columns.equals(this.columns);
	}
}
