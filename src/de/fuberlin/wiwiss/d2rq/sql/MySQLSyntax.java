package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MySQLSyntax extends SQL92Syntax {

	public MySQLSyntax() {
		super(true);
	}
	
	public String getConcatenationExpression(String[] sqlFragments) {
		StringBuffer result = new StringBuffer("CONCAT(");
		for (int i = 0; i < sqlFragments.length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(sqlFragments[i]);
		}
		result.append(")");
		return result.toString();
	}

	public String quoteIdentifier(String identifier) {
		return backtickQuote(identifier);
	}

	/**
	 * Wraps s in backticks and escapes special characters to avoid SQL injection
	 */
	protected String backtickQuote(String s) {
		return "`" + backtickEscapePatternMySQL.matcher(s).
				replaceAll("$1$1") + "`";
	}
	private final static Pattern backtickEscapePatternMySQL = Pattern.compile("([\\\\`])");
	
	public Properties getDefaultConnectionProperties() {
		Properties result = new Properties();
		result.setProperty("autoReconnect", "true");
		result.setProperty("zeroDateTimeBehavior", "convertToNull");
		return result;
	}
}
