package org.d2rq.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.values.TemplateValueMaker;
import org.d2rq.values.TemplateValueMaker.ColumnFunction;


/**
 * A pattern that combines one or more database columns into a String. Often
 * used as an UriPattern for generating URIs from a column's primary key.
 *
 * Patterns consist of alternating literal parts and column references.
 * The D2RQ syntax encloses column references
 * in <code>@@...@@</code>. Column names must be fully qualified.
 * 
 * Example: <code>aaa@@t.col1@@bbb@@t.col2@@@@t.col3@@ccc</code>
 *
 * This has four literal parts: "aaa", "bbb", "", "ccc".
 * It has three column references: t.col1, t.col2, t.col3
 * 
 * Each column reference can also include an encoding function, an instance
 * of {@link ColumnFunction}: <code>aaa@@t.col1|urlify@@bbb</code>.
 * The default encoding function is {@link #IDENTITY}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Pattern {
	public final static String DELIMITER = "@@";

	private final String pattern;
	private final List<String> literalParts = new ArrayList<String>();
	private final List<ColumnName> columns = new ArrayList<ColumnName>();
	private final List<ColumnFunction> functions = new ArrayList<ColumnFunction>();

	public Pattern(String pattern) {
		if (pattern == null) {
			throw new IllegalArgumentException("Pattern was null");
		}
		this.pattern = pattern;
		parse();
	}
	
	private void parse() {
		Matcher match = embeddedColumnRegex.matcher(pattern);
		boolean matched = match.find();
		int firstLiteralEnd = matched ? match.start() : pattern.length();
		literalParts.add(pattern.substring(0, firstLiteralEnd));
		while (matched) {
			columns.add(Microsyntax.parseColumn(match.group(1)));
			functions.add(getColumnFunction(match.group(2)));
			int nextLiteralStart = match.end();
			matched = match.find();
			int nextLiteralEnd = matched ? match.start() : pattern.length();
			literalParts.add(pattern.substring(nextLiteralStart, nextLiteralEnd));
		}
	}
	private final static java.util.regex.Pattern embeddedColumnRegex = 
		java.util.regex.Pattern.compile("@@([^@]+?)(?:\\|(urlencode|urlify|encode))?@@");

	public int getColumnCount() {
		return columns.size();
	}
	
	public String[] getLiteralParts() {
		return literalParts.toArray(new String[literalParts.size()]);
	}
	
	public ColumnName[] getColumns() {
		return columns.toArray(new ColumnName[columns.size()]);
	}
	
	public ColumnFunction[] getFunctions() {
		return functions.toArray(new ColumnFunction[functions.size()]);
	}
	
	public boolean literalPartsMatchRegex(String regex) {
		if (!literalParts.get(0).matches(regex)) {
			return false;
		}
		for (String literalPart: literalParts) {
			if (!literalPart.matches(regex)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Creates a {@link TemplateValueMaker} from a pattern syntax string.
	 * 
	 * TODO: Should receive a {@link DatabaseOp} instead of {@link SQLConnection} to source its attributes
	 */
	public TemplateValueMaker toTemplate(SQLConnection sqlConnection) {
		return new TemplateValueMaker(getLiteralParts(), getColumns(), getFunctions());
	}
	
	public String toString() {
		return pattern;
	}

	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof Pattern)) {
			return false;
		}
		Pattern other = (Pattern) otherObject;
		return pattern.equals(other.pattern);
	}
	
	public int hashCode() {
		return pattern.hashCode();
	}
	
	private static ColumnFunction getColumnFunction(String functionName) {
		if (functionName == null || "".equals(functionName)) {
			return TemplateValueMaker.IDENTITY;
		}
		if ("urlencode".equals(functionName)) {
			return TemplateValueMaker.URLENCODE;
		}
		if ("urlify".equals(functionName)) {
			return TemplateValueMaker.URLIFY;
		}
		if ("encode".equals(functionName)) {
			return TemplateValueMaker.ENCODE;
		}
		// Shouldn't happen
		throw new D2RQException("Unrecognized column function '" + functionName + "'");
	}
	
	public static String toPatternString(String[] literalParts, 
			ColumnName[] columns, ColumnFunction[] functions) {
		StringBuilder s = new StringBuilder(literalParts[0]);
		for (int i = 0; i < columns.length; i++) {
			s.append(DELIMITER);
			s.append(Microsyntax.toString(columns[i]));
			if (functions[i].name() != null) {
				s.append("|");
				s.append(functions[i].name());
			}
			s.append(DELIMITER);
			s.append(literalParts[i + 1]);
		}
		return s.toString();
	}
}