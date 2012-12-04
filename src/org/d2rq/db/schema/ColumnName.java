package org.d2rq.db.schema;

import java.util.Arrays;

import org.d2rq.db.vendor.Vendor;




public class ColumnName implements Comparable<ColumnName> {

	public static ColumnName parse(String s) {
		Identifier.Parser parser = new Identifier.Parser(s);
		if (parser.error() != null) {
			throw new IllegalArgumentException(
					"Malformed column name " + s + ": " + parser.message());
		}
		return create(parser.result());
	}
	
	public static ColumnName create(Identifier[] parts) {
		if (parts.length > 4) {
			throw new IllegalArgumentException(
					"Malformed column name: Too many qualifiers: " + Arrays.toString(parts));
		}
		return create(
				parts.length == 4 ? parts[0] : null,
				parts.length >= 3 ? parts[parts.length - 3] : null,
				parts.length >= 2 ? parts[parts.length - 2] : null,
				parts.length >= 1 ? parts[parts.length - 1] : null);
	}

	public static ColumnName create(Identifier catalog, 
			Identifier schema, Identifier table, Identifier column) {
		return catalog == null && schema == null && table == null
				? create(column)
				: create(TableName.create(catalog, schema, table), column);
	}
	
	public static ColumnName create(Identifier column) {
		return create(null, column);
	}
	
	public static ColumnName create(TableName table, Identifier column) {
		return new ColumnName(table, column);
	}
	
	private final TableName qualifier;
	private final Identifier column;
	
	protected ColumnName(TableName qualifier, Identifier column) {
		if (column == null) {
			throw new IllegalArgumentException("Column identifier was empty or null");
		}
		this.qualifier = qualifier;
		this.column = column;
	}
	
	public Identifier getColumn() {
		return column;
	}
	
	public boolean isQualified() {
		return qualifier != null;
	}
	
	public TableName getQualifier() {
		return qualifier;
	}

	public ColumnName getUnqualified() {
		return isQualified() ? ColumnName.create(column) : this;
	}
	
	@Override
	public String toString() {
		return Vendor.SQL92.toString(this);
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof ColumnName)) return false;
		ColumnName other = (ColumnName) otherObject;
		if (other.qualifier == null) {
			if (qualifier != null) return false; 
		} else {
			if (!other.qualifier.equals(qualifier)) return false;
		}
		return other.column.equals(column);
	}
	
	@Override
	public int hashCode() {
		return (qualifier == null ? 4345 : qualifier.hashCode()) ^ column.hashCode() ^ 584325;
	}

	public int compareTo(ColumnName other) {
		if (qualifier == null) {
			if (other.qualifier != null) return -1;
		} else {
			if (other.qualifier == null) return 1;
			int i = qualifier.compareTo(other.qualifier);
			if (i != 0) return i;
		}
		return column.compareTo(other.column);
	}
}
