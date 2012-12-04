package org.d2rq.r2rml;

import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.schema.Identifier.Parser.ViolationType;

/**
 * A SQL table or view name. May be qualified with a catalog name and schema name.
 * 
 * @see http://www.w3.org/TR/r2rml/#dfn-table-or-view-name
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TableOrViewName extends MappingTerm {
	
	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @result <code>null</code> if arg is <code>null</code>
	 */
	public static TableOrViewName create(String tableName) {
		return tableName == null ? null : new TableOrViewName(tableName);
	}
	
	private final String asString;
	private final TableName parsed;
	private ViolationType error = null;
	private String message = null;
	
	private TableOrViewName(String s) {
		asString = s.trim();
		Identifier.Parser parser = new Identifier.Parser(asString);
		if (parser.error() != null) {
			error = parser.error();
			message = parser.message();
			parsed = null;
		} else if (parser.countParts() > 3) {
			error = ViolationType.TOO_MANY_IDENTIFIERS;
			message = "Too many identifiers; must be in [catalog.][schema.]table form";
			parsed = null;
		} else {
			parsed = TableName.create(parser.result());
		}
	}

	public TableName asQualifiedTableName() {
		return parsed;
	}
	
	/**
	 * A form appropriate for use in SQL queries, if valid.
	 */
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
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}

	@Override
	public boolean isValid() {
		return error == null;
	}

	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof TableOrViewName)) return false;
		TableOrViewName other = (TableOrViewName) otherObject;
		if (isValid() != other.isValid()) return false;
		return isValid() ? parsed.equals(other.parsed) : asString.equals(other.asString);
	}

	@Override
	public int hashCode() {
		return (isValid() ? parsed.hashCode() : asString.hashCode()) ^ 284;
	}
}
