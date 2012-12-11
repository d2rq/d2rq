package org.d2rq.r2rml;

import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.Parser.ViolationType;
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

	public static ColumnNameR2RML create(Identifier name) {
		return name == null ? null : new ColumnNameR2RML(Vendor.SQL92.toString(name));
	}
	
	private final String asString;
	private final Identifier parsed;
	private ViolationType error;
	private String message;
	
	private ColumnNameR2RML(String name) {
		asString = name.trim();
		Identifier.Parser parser = new Identifier.Parser(asString);
		if (parser.error() == null) {
			if (parser.result().length > 1) {
				parsed = null;
				error = ViolationType.TOO_MANY_IDENTIFIERS;
				message = "Column names must be unqualified";
			} else {
				parsed = parser.result()[0];
			}
		} else {
			parsed = null;
			error = parser.error();
			message = parser.message();
		}
	}
	
	@Override
	public String toString() {
		return asString;
	}
	
	public ViolationType getSyntaxError() {
		return error;
	}
	
	public String getSyntaxErrorMessage() {
		return message;
	}
	
	public Identifier asIdentifier() {
		return parsed;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}
	
	@Override
	public boolean isValid() {
		return error == null;
	}
	
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof ColumnNameR2RML)) return false;
		ColumnNameR2RML other = (ColumnNameR2RML) otherObject;
		return (error == null) ? parsed.equals(other.parsed) : asString.equals(other.asString);
	}
	
	public int hashCode() {
		return ((error == null) ? parsed.hashCode() : asString.hashCode()) ^ 66456;
	}
}
