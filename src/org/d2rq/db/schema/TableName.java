package org.d2rq.db.schema;

import java.util.ArrayList;
import java.util.List;

import org.d2rq.db.vendor.Vendor;


/**
 * A table name, possibly qualified with schema and catalog.
 * 
 * @author Richard Cyganiak (richad@cyganiak.de)
 */
public class TableName implements Comparable<TableName> {

	public static TableName parse(String s) {
		Identifier.Parser parser = new Identifier.Parser(s);
		if (parser.error() != null) {
			throw new IllegalArgumentException(
					"Malformed qualified table name " + s + ": " + parser.message());
		}
		if (parser.countParts() > 3) {
			throw new IllegalArgumentException(
					"Malformed qualified table name " + s + ": Too many qualifiers");
		}
		return create(parser.result());
	}
	
	public static TableName create(Identifier catalog, Identifier schema, Identifier table) {
		return new TableName(catalog, schema, table);
	}
	
	public static TableName create(Identifier[] parts) {
		if (parts.length == 0 || parts.length > 3) {
			throw new IllegalArgumentException(parts.length + " identifiers");
		}
		return new TableName(
				parts.length == 3 ? parts[0] : null,
				parts.length >= 2 ? parts[parts.length - 2] : null,
				parts[parts.length - 1]);
	}
	
	private final Identifier catalog;
	private final Identifier schema;
	private final Identifier table;
	
	private TableName(Identifier catalog, Identifier schema, Identifier table) {
		this.catalog = catalog;
		this.schema = schema;
		this.table = table;
	}
	
	public Identifier getCatalog() {
		return catalog;
	}
	
	public Identifier getSchema() {
		return schema;
	}
	
	public Identifier getTable() {
		return table;
	}
	
	public ColumnName qualifyIdentifier(Identifier identifier) {
		return ColumnName.create(this, identifier);
	}
	
	public ColumnName qualifyColumn(ColumnName column) {
		if (column.isQualified() && column.getQualifier().equals(this)) {
			return column;
		}
		return ColumnName.create(this, column.getColumn());
	}

	public List<ColumnName> qualifyIdentifiers(List<Identifier> identifiers) {
		List<ColumnName> result = new ArrayList<ColumnName>(identifiers.size());
		for (Identifier identifier: identifiers) {
			result.add(qualifyIdentifier(identifier));
		}
		return result;
	}
	
	public TableName withPrefix(int index) {
		// FIXME: This could clash if we have A_B.C and A.B_C or A_B_C; all yield T1_A_B_C
		String name = "T" + index + "_" + 
				(schema == null ? "" : schema.getName() + "_") +
				table.getName();
		/*
		 * Oracle can't handle identifier names longer than 30 characters.
		 * To prevent the oracle error "ORA-00972: identifier is too long"
		 * we need to cut those longer names off but keep them unique.
		 * 
		 * TODO: Make this dependent on whether we're dealing with an Oracle
		 * database or not.
		 */
		if (name.length() > 30) {
			name = "T" + index + "_" + Integer.toHexString(name.hashCode());
		}
		return TableName.create(null, null, Identifier.createDelimited(name));		
	}

	@Override
	public String toString() {
		return Vendor.SQL92.toString(this);
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof TableName)) return false;
		TableName other = (TableName) otherObject;
		if (other.catalog == null) {
			if (catalog != null) return false;
		} else {
			if (!other.catalog.equals(catalog)) return false;
		}
		if (other.schema == null) {
			if (schema != null) return false;
		} else {
			if (!other.schema.equals(schema)) return false;
		}
		return other.table.equals(table);
	}
	
	@Override
	public int hashCode() {
		return (catalog == null ? 134134 : catalog.hashCode()) ^ 
				(schema == null ? 8589 : schema.hashCode()) ^ table.hashCode();
	}

	public int compareTo(TableName other) {
		if (catalog == null || other.catalog == null) {
			if (catalog == null && other.catalog != null) return -1;
			if (catalog != null && other.catalog == null) return 1;
		} else {
			int i = catalog.compareTo(other.catalog);
			if (i != 0) return i;
		}
		if (schema == null || other.schema == null) {
			if (schema == null && other.schema != null) return -1;
			if (schema != null && other.schema == null) return 1;
		} else {
			int i = schema.compareTo(other.schema);
			if (i != 0) return i;
		}
		return table.compareTo(other.table);
	}
}
