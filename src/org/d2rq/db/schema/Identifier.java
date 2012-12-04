package org.d2rq.db.schema;

import java.util.ArrayList;
import java.util.List;

import org.d2rq.db.vendor.Vendor;




/**
 * The name of a SQL object, such as a column, table, view, schema, or catalog.
 * This may include appropriate quotation marks around the identifier.
 * A SQL identifier must match the "identifier" production in [SQL2].
 * When comparing identifiers for equality, the comparison rules of [SQL2] must be used.
 * 
 * "A", a and A are all considered equal for purposes of
 * {@link #equals} and {@link #compareTo}.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Identifier implements Comparable<Identifier> {
	
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
	 * @param isDelimited Is this a delimited identifier as per SQL spec?
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

	public static class Parser {

		public enum ViolationType {
			EMPTY_DELIMITED_IDENTIFIER,
			TOO_MANY_IDENTIFIERS,
			UNEXPECTED_CHARACTER,
			UNEXPECTED_END
		}

		private enum ParserState { 
			IDENTIFIER_START, 
			IN_DELIMITED_IDENTIFIER, 
			DELIMITED_IDENTIFIER_END,
			IN_UNDELIMITED_IDENTIFIER
		}

		private final String input;
		private final List<Identifier> result = new ArrayList<Identifier>(3);
		private ParserState state = ParserState.IDENTIFIER_START;
		private int position = 0;
		private StringBuilder buffer = new StringBuilder();
		private ViolationType error = null;
		private String message = null;
		
		public Parser(String s) {
			input = s;
			parse();
		}
		
		/**
		 * @return <code>null</code> on error
		 */
		public Identifier[] result() {
			return (error == null) ? result.toArray(new Identifier[result.size()]) : null; 
		}
		
		public int countParts() {
			return result.size();
		}
		
		/**
		 * @return <code>null</code> if no error
		 */
		public ViolationType error() {
			return error;
		}
		
		public String message() {
			return message;
		}

		private void parse() {
			while (position <= input.length()) {
				boolean atEnd = (position == input.length());
				char c = atEnd ? '.' : input.charAt(position);
				switch (state) {
				case IDENTIFIER_START:
					if (c == '\"') {
						state = ParserState.IN_DELIMITED_IDENTIFIER;
					} else if (isIdentifierStartChar(c)) {
						state = ParserState.IN_UNDELIMITED_IDENTIFIER;
						buffer.append(c);
					} else if (atEnd) {
						error = ViolationType.UNEXPECTED_END;
						message = "Unexpected end; expected a SQL identifier";
						return;
					} else {
						error = ViolationType.UNEXPECTED_CHARACTER;
						message = "Unexpected character '" + c + "' at " + (position + 1) + " in SQL identifier";
						return;
					}
					break;
				case IN_DELIMITED_IDENTIFIER:
					if (c == '\"') {
						state = ParserState.DELIMITED_IDENTIFIER_END;
					} else if (atEnd) {
						error = ViolationType.UNEXPECTED_END;
						message = "Unexpected end; expected closing delimiter";
						return;
					} else {
						buffer.append(c);
					}
					break;
				case DELIMITED_IDENTIFIER_END:
					if (c == '\"') {
						state = ParserState.IN_DELIMITED_IDENTIFIER;
						buffer.append(c);
					} else if (c == '.') {
						if (buffer.length() == 0) {
							error = ViolationType.EMPTY_DELIMITED_IDENTIFIER;
							message = "Empty delimited identifier";
							return;
						}
						state = ParserState.IDENTIFIER_START;
						result.add(new Identifier(true, buffer.toString()));
						buffer = new StringBuilder();
					} else {
						error = ViolationType.UNEXPECTED_CHARACTER;
						message = "Unexpected character '" + c + "' at " + (position + 1) + " in SQL identifier";
						return;
					}
					break;
				case IN_UNDELIMITED_IDENTIFIER:
					if (c == '.') {
						state = ParserState.IDENTIFIER_START;
						result.add(new Identifier(false, buffer.toString()));
						buffer = new StringBuilder();
					} else if (isIdentifierBodyChar(c)) {
						buffer.append(c);
					} else {
						error = ViolationType.UNEXPECTED_CHARACTER;
						message = "Unexpected character '" + c + "' at " + (position + 1) + " in SQL identifier";
						return;
					}
					break;
				}
				position++;
			}
		}
	
		/**
		 * Regular identifiers must start with a Unicode character from any of the 
		 * following character classes: upper-case letter, lower-case letter, 
		 * title-case letter, modifier letter, other letter, or letter number.
		 * 
		 * @see http://www.w3.org/TR/r2rml/#dfn-sql-identifier
		 */
		private boolean isIdentifierStartChar(char c) {
			int category = Character.getType(c);
			for (byte b: identifierStartCategories) {
				if (category == b) return true;
			}
			return false;
		}
		private byte[] identifierStartCategories = {
				Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
				Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
				Character.OTHER_LETTER, Character.LETTER_NUMBER
		};
	
		/**
		 * Subsequent characters may be any of these, or a nonspacing mark, 
		 * spacing combining mark, decimal number, connector punctuation, 
		 * and formatting code.
		 * 
		 * @see http://www.w3.org/TR/r2rml/#dfn-sql-identifier
		 */
		private boolean isIdentifierBodyChar(char c) {
			if (isIdentifierStartChar(c)) return true;
			int category = Character.getType(c);
			for (byte b: identifierBodyCategories) {
				if (category == b) return true;
			}
			return false;
		}
		private byte[] identifierBodyCategories = {
				Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK,
				Character.DECIMAL_DIGIT_NUMBER, Character.CONNECTOR_PUNCTUATION,
				Character.FORMAT
		};
	}
}
