package org.d2rq.r2rml;

import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.vendor.Vendor;

/**
 * A column name is the name of a column of a logical table. 
 * A column name must be a valid SQL identifier. 
 * Column names do not include any qualifying table, view or schema names.
 * 
 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-column-name">R2RML: Column name</a>
 */
public class ColumnNameR2RML extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @return <code>null</code> if arg is <code>null</code>
	 */
	public static ColumnNameR2RML create(String name) {
		return name == null ? null : new ColumnNameR2RML(name);
	}

	public static ColumnNameR2RML create(Identifier name, Vendor vendor) {
		return name == null ? null : new ColumnNameR2RML(vendor.toString(name));
	}
	
	private final String asString;
	
	private ColumnNameR2RML(String name) {
		asString = name.trim();
	}

	public Identifier asIdentifier(Vendor vendor) {
		try {
			return vendor.parseIdentifiers(asString, 1, 1)[0];
		} catch (IdentifierParseException ex) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return asString;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof ColumnNameR2RML)) return false;
		ColumnNameR2RML other = (ColumnNameR2RML) otherObject;
		return asString.equals(other.asString);
	}
	
	public int hashCode() {
		return asString.hashCode() ^ 66456;
	}
}
