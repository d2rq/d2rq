package org.d2rq.r2rml;

import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.vendor.Vendor;

/**
 * A SQL table or view name. May be qualified with a catalog name and schema name.
 * 
 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-table-or-view-name">R2RML: Table or view name</a>
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TableOrViewName extends MappingTerm {
	
	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @return <code>null</code> if arg is <code>null</code>
	 */
	public static TableOrViewName create(String tableName) {
		return tableName == null ? null : new TableOrViewName(tableName);
	}
	
	public static TableOrViewName create(TableName tableName, Vendor vendor) {
		return tableName == null ? null : new TableOrViewName(vendor.toString(tableName));
	}
	
	private final String asString;
	
	private TableOrViewName(String s) {
		asString = s.trim();
	}

	public TableName asQualifiedTableName(Vendor vendor) {
		try {
			return TableName.create(vendor.parseIdentifiers(asString, 1, 3)); 
		} catch (IdentifierParseException ex) {
			return null;
		}
	}
	
	/**
	 * A form appropriate for use in SQL queries, if valid.
	 */
	@Override
	public String toString() {
		return asString;
	}

	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}

	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof TableOrViewName)) return false;
		TableOrViewName other = (TableOrViewName) otherObject;
		return asString.equals(other.asString);
	}

	@Override
	public int hashCode() {
		return asString.hashCode() ^ 284;
	}
}
