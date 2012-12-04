package org.d2rq.db.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A list of {@link Identifier}s for use as keys, e.g., unique keys.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Key implements Iterable<Identifier>, Comparable<Key> {
	
	/**
	 * Creates an instance from a list of columns, ignoring identifiers.
	 */
	public static Key create(ColumnName... columns) {
		List<Identifier> unqualifiedColumns = new ArrayList<Identifier>();
		for (ColumnName column: columns) {
			unqualifiedColumns.add(column.getColumn());
		}
		return new Key(unqualifiedColumns);
	}

	/**
	 * Creates an instance from a list of columns, ignoring identifiers.
	 */
	public static Key createFromColumns(List<ColumnName> columns) {
		return Key.create(columns.toArray(new ColumnName[columns.size()]));
	}

	public static Key create(Identifier... columns) {
		return new Key(Arrays.asList(columns));
	}
	
	public static Key createFromIdentifiers(List<Identifier> columns) {
		return new Key(columns);
	}
	
	private final List<Identifier> columns;
	
	private Key(List<Identifier> columns) {
		this.columns = columns;
	}
	
	public List<Identifier> getColumns() {
		return columns;
	}

	/**
	 * Ignores ambiguous columns and qualifiers.
	 */
	public boolean isContainedIn(Collection<ColumnName> columns) {
		for (Identifier ourColumn: this) {
			boolean found = false;
			for (ColumnName theirColumn: columns) {
				if (found) {
					// Ambiguous column
					return false;
				}
				if (theirColumn.getColumn().equals(ourColumn)) {
					found = true;
				}
			}
			if (!found) return false;
		}
		return true;
	}
	
	public int size() {
		return columns.size();
	}
	
	public Iterator<Identifier> iterator() {
		return columns.iterator();
	}
	
	public boolean contains(Identifier column) {
		return columns.contains(column);
	}
	
	public Identifier get(int index) {
		return columns.get(index);
	}
	
	@Override
	public String toString() {
		return "Key" + columns;
	}
	
	@Override
	public int hashCode() {
		return columns.hashCode() ^ 596034;
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof Key) && columns.equals(((Key) o).columns);
	}

	public int compareTo(Key o) {
		if (size() < o.size()) return -1;
		if (size() > o.size()) return 1;
		for (int i = 0; i < size(); i++) {
			int compare = get(i).compareTo(o.get(i));
			if (compare < 0) return -1;
			if (compare > 0) return 1;
		}
		return 0;
	}
}
