package org.d2rq.db.schema;

import org.d2rq.db.vendor.Vendor;




/**
 * The name of a SQL object, such as a column, table, view, schema, or catalog.
 * This may include appropriate quotation marks around the identifier.
 * A SQL identifier must match the "identifier" production in [SQL2].
 * When comparing identifiers for equality, the comparison rules of [SQL2] must be used.
 * 
 * "A", a and A are all considered equal for purposes of
 * {@link #equals} and {@link #compareTo}, but "a" equals neither of them.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Identifier implements Comparable<Identifier> {
	
	public enum ViolationType {
		EMPTY_DELIMITED_IDENTIFIER,
		TOO_MANY_IDENTIFIERS,
		UNEXPECTED_CHARACTER,
		UNEXPECTED_END
	}

	/**
	 * Caller must ensure that the name is valid.
	 * @param name The identifier's name, without delimiters and unescaped
	 * @return A matching Identifier instance, or <code>null</code> if name was null
	 */
	public static Identifier createDelimited(String name) {
		return name == null ? null : new Identifier(true, name);
	}
	
	/**
	 * Caller must ensure that the name is valid.
	 * @param name The identifier's name, without delimiters and unescaped
	 * @return A matching Identifier instance, or <code>null</code> if name was null
	 */
	public static Identifier createUndelimited(String name) {
		return name == null ? null : new Identifier(false, name);
	}
	
	/**
	 * Caller must ensure that the name is valid.
	 * @param delimited Is this a delimited identifier as per SQL spec?
	 * @param name The identifier's name, without delimiters and unescaped
	 * @return A matching Identifier instance, or <code>null</code> if name was null
	 */
	public static Identifier create(boolean delimited, String name) {
		return name == null ? null : new Identifier(delimited, name);
	}
	
	private boolean isDelimited;
	private String name;

	private Identifier(boolean isDelimited, String name) {
		if (name == null) throw new IllegalArgumentException();
		this.isDelimited = isDelimited;
		this.name = name;
	}
	
	/**
	 * General display name. Does not include delimiters.
	 * Not suitable for use in SQL queries.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Case-normalized form if case-insensitive. Does not include delimiters.
	 * Not suitable for use in SQL queries as no escaping is performed.
	 */
	public String getCanonicalName() {
		return isDelimited ? name : name.toUpperCase();
	}
	
	/**
	 * Is this a delimited identifier (case-sensitive, written with
	 * double-quotes in standard SQL)?
	 */
	public boolean isDelimited() {
		return isDelimited;
	}
	
	@Override
	public String toString() {
		return Vendor.SQL92.toString(this);
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof Identifier)) return false;
		Identifier other = (Identifier) otherObject;
		return getCanonicalName().equals(other.getCanonicalName());
	}
	
	@Override
	public int hashCode() {
		return getCanonicalName().hashCode() ^ 76463;
	}

	public int compareTo(Identifier o) {
		return getCanonicalName().compareTo(o.getCanonicalName());
	}

	public static class IdentifierParseException extends Exception {
		private final ViolationType violationType;
		public IdentifierParseException(ViolationType errorType, String errorMessage) {
			super(errorMessage);
			violationType = errorType;
		}
		public ViolationType getViolationType() {
			return violationType;
		}
	}
}
