package org.d2rq.db.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.schema.Identifier.ViolationType;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.SQLApproximateNumeric;
import org.d2rq.db.types.SQLBinary;
import org.d2rq.db.types.SQLBit;
import org.d2rq.db.types.SQLBoolean;
import org.d2rq.db.types.SQLCharacterString;
import org.d2rq.db.types.SQLCharacterStringVarying;
import org.d2rq.db.types.SQLDate;
import org.d2rq.db.types.SQLExactNumeric;
import org.d2rq.db.types.SQLTime;
import org.d2rq.db.types.SQLTimestamp;
import org.d2rq.db.types.UnsupportedDataType;
import org.d2rq.lang.Database;


/**
 * This base class implements SQL-92 compatible syntax. Subclasses
 * can override individual methods to implement different syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQL92 implements Vendor {

	private boolean useAS;
	
	/**
	 * Initializes a new instance.
	 * 
	 * @param useAS Use "Table AS Alias" or "Table Alias" in FROM clauses? In standard SQL, either is fine.
	 */
	public SQL92(boolean useAS) {
		this.useAS = useAS;
	}
	
	public String getConcatenationExpression(String[] sqlFragments) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < sqlFragments.length; i++) {
			if (i > 0) {
				result.append(" || ");
			}
			result.append(sqlFragments[i]);
		}
		return result.toString();
	}

	public String getAliasOperator() {
		return useAS ? " AS " : " ";
	}
	
	public String getTrueTable() {
		return "(VALUES(NULL))";
	}
	
	public Identifier[] parseIdentifiers(String s, int minParts, int maxParts) 
	throws IdentifierParseException {
		IdentifierParser parser = new IdentifierParser(s, minParts, maxParts);
		if (parser.error() == null) {
			return parser.result();
		} else {
			throw new IdentifierParseException(parser.error(), parser.message());
		}
	}
	
	public String toString(Identifier identifier) {
		return identifier.isDelimited() 
				? doubleQuoteEscaper.quote(identifier.getName()) 
				: identifier.getName();
	}
	private final static Quoter doubleQuoteEscaper = 
		new PatternDoublingQuoter(Pattern.compile("(\")"), "\"");
	
	public String toString(ColumnName column) {
		return (!column.isQualified() ? "" : toString(column.getQualifier()) + ".") +
				toString(column.getColumn());
	}

	public String toString(TableName table) {
		return (table.getCatalog() == null ? "" : toString(table.getCatalog()) + ".") +
				(table.getSchema() == null ? "" : toString(table.getSchema()) + ".") +
				toString(table.getTable());
	}

	public String quoteStringLiteral(String s) {
		return singleQuoteEscaper.quote(s);
	}
	private final static Quoter singleQuoteEscaper = 
		new PatternDoublingQuoter(Pattern.compile("(')"), "'");

	public String quoteBinaryLiteral(String hexString) {
		return "X" + quoteStringLiteral(hexString);
	}
	
	public String quoteDateLiteral(String date) {
		return "DATE " + quoteStringLiteral(date);
	}
	
	public String quoteTimeLiteral(String time) {
		return "TIME " + quoteStringLiteral(time);
	}
	
	public String quoteTimestampLiteral(String timestamp) {
		return "TIMESTAMP " + quoteStringLiteral(timestamp);
	}

	public Expression getRowNumLimitAsExpression(int limit) {
		return Expression.TRUE;
	}

	/**
	 * Technically speaking, SQL 92 supports NO way of limiting
	 * result sets (ROW_NUMBER appeared in SQL 2003). We will
	 * just use MySQL's LIMIT as it appears to be widely implemented.
	 */
	public String getRowNumLimitAsQueryAppendage(int limit) {
		if (limit == Database.NO_LIMIT) return "";
		return "LIMIT " + limit;
	}

	public String getRowNumLimitAsSelectModifier(int limit) {
		return "";
	}

	public Properties getDefaultConnectionProperties() {
		return new Properties();
	}
	
	public DataType getDataType(int jdbcType, String name, int size) {
		// TODO: These are in java.sql.Types as of Java 6 but not yet in Java 1.5
		if ("NCHAR".equals(name) || "NVARCHAR".equals(name) || "NCLOB".equals(name)) {
			return new SQLCharacterStringVarying(name, true);
		}

		
		switch (jdbcType) {
		case Types.CHAR:
			return new SQLCharacterString(name, true);

		case Types.VARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB:
			return new SQLCharacterStringVarying(name, true);
			
		case Types.BOOLEAN:
			return new SQLBoolean(name);

		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return new SQLBinary(name, true);
			
		case Types.BIT:
			return new SQLBit(name);

		case Types.NUMERIC:
		case Types.DECIMAL:
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
		case Types.BIGINT:
			return new SQLExactNumeric(name, jdbcType, false);
			
		case Types.REAL:
		case Types.FLOAT:
		case Types.DOUBLE:
			return new SQLApproximateNumeric(name);
		
		case Types.DATE:
			return new SQLDate(name);
			
		case Types.TIME:
			return new SQLTime(name);
			
		case Types.TIMESTAMP:
			return new SQLTimestamp(name);

		case Types.ARRAY:
		case Types.JAVA_OBJECT:
			return new UnsupportedDataType(jdbcType, name);
			
		// TODO: What about the remaining java.sql.Types?
		case Types.DATALINK:
		case Types.DISTINCT:
		case Types.NULL:
		case Types.OTHER:
		case Types.REF:
		}
		
		return null;
	}

	/**
	 * In most databases, we don't have to do anything because boolean
	 * expressions are allowed anywhere.
	 */
	public Expression booleanExpressionToSimpleExpression(Expression expression) {
		return expression;
	}
	
	public boolean isIgnoredTable(String catalog, String schema, String table) {
		return false;
	}

	public TableName toQualifiedTableName(String catalog, String schema, String table) {
		return TableName.create(
				Identifier.createDelimited(catalog), 
				Identifier.createDelimited(schema),
				Identifier.createDelimited(table));
	}
	
	public void initializeConnection(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public interface Quoter {
		public abstract String quote(String s);
	}
	
	public static class PatternDoublingQuoter implements Quoter {
		private final Pattern pattern;
		private final String quote;
		public PatternDoublingQuoter(Pattern pattern, String quote) {
			this.pattern = pattern;
			this.quote = quote;
		}
		public String quote(String s) {
			return quote + pattern.matcher(s).replaceAll("$1$1") + quote;
		}
	};
	/**
	 * Parser for standard SQL92 identifiers.
	 */
	public static class IdentifierParser {
		private enum ParserState { 
			IDENTIFIER_START, 
			IN_UNDELIMITED_IDENTIFIER,
			IN_DELIMITED_IDENTIFIER, 
			DELIMITED_IDENTIFIER_END
		}

		private final String input;
		private final int maxParts;
		private final List<Identifier> result = new ArrayList<Identifier>(3);
		private ParserState state = ParserState.IDENTIFIER_START;
		private int position = 0;
		private StringBuilder buffer = new StringBuilder();
		private ViolationType error = null;
		private String message = null;
		
		public IdentifierParser(String s, int minParts, int maxParts) {
			input = s;
			this.maxParts = maxParts;
			parse();
			if (error == null && result.size() < minParts) {
				error = ViolationType.UNEXPECTED_END;
				message = "Unexpected end; expected at least " + 
						minParts + " identifiers";
			}
		}
		
		public Identifier[] result() {
			return (error == null) ? result.toArray(new Identifier[result.size()]) : null; 
		}
		
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
					if (isOpeningQuoteChar(c)) {
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
					if (isClosingQuoteChar(c)) {
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
					if (isOpeningQuoteChar(c)) {
						state = ParserState.IN_DELIMITED_IDENTIFIER;
						buffer.append(c);
					} else if (c == '.') {
						if (buffer.length() == 0) {
							error = ViolationType.EMPTY_DELIMITED_IDENTIFIER;
							message = "Empty delimited identifier";
							return;
						}
						if (!finishIdentifier(true)) return;
						state = ParserState.IDENTIFIER_START;
					} else {
						error = ViolationType.UNEXPECTED_CHARACTER;
						message = "Unexpected character '" + c + "' at " + (position + 1) + " in SQL identifier";
						return;
					}
					break;
				case IN_UNDELIMITED_IDENTIFIER:
					if (c == '.') {
						if (!finishIdentifier(false)) return;
						state = ParserState.IDENTIFIER_START;
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

		private boolean finishIdentifier(boolean delimited) {
			if (result.size() >= maxParts) {
				error = ViolationType.TOO_MANY_IDENTIFIERS;
				message = maxParts == 1
						? "Expected unqualified identifier"
						: "Too many qualifiers";
				return false;
			}
			if (!isValidIdentifier(buffer.toString(), delimited)) {
				error = ViolationType.UNEXPECTED_CHARACTER;
				message = "Failed database-specific identifier validation rule";
				return false;
			}
			result.add(Identifier.create(delimited, buffer.toString()));
			buffer = new StringBuilder();
			return true;
		}
		
		protected boolean isValidIdentifier(String identifier, boolean delimited) {
			return true;
		}
		
		protected boolean isOpeningQuoteChar(char c) {
			return c == '"';
		}
		
		protected boolean isClosingQuoteChar(char c) {
			return c == '"';
		}
		
		/**
		 * Regular identifiers must start with a Unicode character from any of the 
		 * following character classes: upper-case letter, lower-case letter, 
		 * title-case letter, modifier letter, other letter, or letter number.
		 * 
		 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-sql-identifier">R2RML: SQL identifier</a>
		 */
		protected boolean isIdentifierStartChar(char c) {
			int category = Character.getType(c);
			for (byte b: identifierStartCategories) {
				if (category == b) return true;
			}
			return false;
		}
		private final static byte[] identifierStartCategories = {
				Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
				Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
				Character.OTHER_LETTER, Character.LETTER_NUMBER
		};
	
		/**
		 * Subsequent characters may be any of these, or a nonspacing mark, 
		 * spacing combining mark, decimal number, connector punctuation, 
		 * and formatting code.
		 * 
		 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-sql-identifier">R2RML: SQL identifier</a>
		 */
		protected boolean isIdentifierBodyChar(char c) {
			if (isIdentifierStartChar(c)) return true;
			int category = Character.getType(c);
			for (byte b: identifierBodyCategories) {
				if (category == b) return true;
			}
			return false;
		}
		private final static byte[] identifierBodyCategories = {
				Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK,
				Character.DECIMAL_DIGIT_NUMBER, Character.CONNECTOR_PUNCTUATION,
				Character.FORMAT
		};
	}

	public void beforeExecuteQuery(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void afterExecuteQuery(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeExecute(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void afterExecute(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeClose(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void afterClose(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void beforeCancel(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}

	public void afterCancel(Connection connection) throws SQLException {
		// Do nothing for standard SQL 92. Subclasses can override.
	}
}
