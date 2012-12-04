package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.d2rq.db.schema.Identifier;


/**
 * A string template is a format string that can be used to build strings 
 * from multiple components. It can reference column names by enclosing them 
 * in curly braces ("{" and "}").
 * 
 * @see http://www.w3.org/TR/r2rml/#dfn-string-template
 */
public class StringTemplate extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @result <code>null</code> if arg is <code>null</code>
	 */
	public static StringTemplate create(String template) {
		return template == null ? null : new StringTemplate(template);
	}

	private final String asString;
	private final String[] literalParts;
	private final Identifier[] columnNameParts;
	private final String errorName;
	private final String errorMessage;
	
	private StringTemplate(String template) {
		this.asString = template;
		Parser parser = new Parser(asString);
		literalParts = parser.literalParts();
		columnNameParts = parser.columnNameParts();
		errorName = parser.errorName();
		errorMessage = parser.errorMessage();
	}
	
	@Override
	public String toString() {
		return asString;
	}
	
	public String[] getLiteralParts() {
		return literalParts;
	}
	
	public Identifier[] getColumnNames() {
		return columnNameParts;
	}

	public String getSyntaxErrorCode() {
		return errorName;
	}
	
	public String getSyntaxErrorMessage() {
		return errorMessage;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}

	@Override
	public boolean isValid() {
		return errorName == null;
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof StringTemplate)) return false;
		StringTemplate other = (StringTemplate) otherObject;
		if (errorName == null) {
			return Arrays.equals(literalParts, other.literalParts)
					&& Arrays.equals(columnNameParts, other.columnNameParts);
		} else {
			return asString.equals(other.asString);
		}
	}
	
	@Override
	public int hashCode() {
		return ((errorName == null)
				? (Arrays.hashCode(literalParts) ^ Arrays.hashCode(columnNameParts))
				: asString.hashCode()) ^ 684873;
	}
	
	private static class Parser {
		private final String input;
		private int position = 1;
		private int lastOpenCurly = -1;
		private List<String> literalParts = new ArrayList<String>();
		private List<Identifier> columnParts = new ArrayList<Identifier>();
		private StringBuilder buffer = new StringBuilder();
		private boolean inColumnPart = false;
		private boolean seenBackslash = false;
		private String errorName = null;
		private String errorMessage = null;
		Parser(String input) {
			this.input = input;
			parse();
		}
		String[] literalParts() {
			return literalParts.toArray(new String[literalParts.size()]);
		}
		Identifier[] columnNameParts() {
			return columnParts.toArray(new Identifier[columnParts.size()]);
		}
		String errorName() {
			return errorName;
		}
		String errorMessage() {
			return errorMessage;
		}
		private void parse() {
			while (position <= input.length() && errorName == null) {
				// position is 1-based for simpler error reporting
				parseCharacter(input.charAt(position - 1));
				position++;
			}
			if (errorName != null) return;
			if (seenBackslash) {
				error("UNDOUBLED_BACKSLASH", "Backslash must be doubled at " + (position - 1));
			} else if (inColumnPart) {
				error("UNMATCHED_CURLY", "Unmatched opening curly brace at " + lastOpenCurly);
			} else {
				finishLiteralPart();
			}
		}
		private void parseCharacter(char c) {
			if (seenBackslash) {
				seenBackslash = false;
				if (c == '{' || c == '}' || c == '\\') {
					buffer.append(c);
				} else {
					error("UNDOUBLED_BACKSLASH", "Backslash must be doubled at " + (position - 1));
				}
			} else if (c == '{') {
				if (inColumnPart) {
					error("UNMATCHED_CURLY", "Double opening curly brace at " + position);
				} else {
					finishLiteralPart();
					inColumnPart = true;
					lastOpenCurly = position;
				}
			} else if (c == '}') {
				if (inColumnPart) {
					finishColumnPart();
					inColumnPart = false;
				} else {
					error("UNMATCHED_CURLY", "Unmatched closing curly brace at " + position);
				}
			} else if (c == '\\') {
				seenBackslash = true;
			} else {
				buffer.append(c);
			}
		}
		private void finishLiteralPart() {
			literalParts.add(buffer.toString());
			buffer = new StringBuilder();
		}
		private void finishColumnPart() {
			Identifier.Parser columnParser = new Identifier.Parser(buffer.toString());
			if (columnParser.error() == null) {
				if (columnParser.result().length > 1) {
					error("QUALIFIED_COLUMN_NAME", "Column name must be unqualified");
				} else {
					columnParts.add(columnParser.result()[0]);
				}
			} else {
				error(columnParser.error().name(), columnParser.message() + " in column name");
			}
			buffer = new StringBuilder();
		}
		private void error(String name, String message) {
			this.errorName = name;
			this.errorMessage = message;
		}
	}
}
