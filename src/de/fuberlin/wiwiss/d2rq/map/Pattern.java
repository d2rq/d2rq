package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A pattern that combines one or more database columns into a String. Often
 * used as an UriPattern for generating URIs from a column's primary key.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Pattern.java,v 1.9 2006/09/09 23:25:14 cyganiak Exp $
 */
public class Pattern implements ValueSource {
	public final static String DELIMITER = "@@";
	private final static java.util.regex.Pattern embeddedColumnRegex = 
		java.util.regex.Pattern.compile("@@([^@]+)@@");

	private String pattern;
	private String firstLiteralPart;
	private List columns = new ArrayList(3);
	private List literalParts = new ArrayList(3);
	private Set columnsAsSet;
	private java.util.regex.Pattern regex;
	
	/**
	 * Constructs a new Pattern instance from a pattern syntax string
	 * @param pattern a pattern syntax string
	 * @throws D2RQException on malformed pattern
	 */
	public Pattern(String pattern) {
		this.pattern = pattern;
		parsePattern();
		this.columnsAsSet = new HashSet(this.columns);
	}

	public void matchConstraint(NodeConstraint c) {
		c.matchPattern(this, this.columns);
	}

	public boolean couldFit(String value) {
		if (value == null) {
			return false;
		}
		return this.regex.matcher(value).matches();
	}

	public Set getColumns() {
		return this.columnsAsSet;
	}

	/**
	 * Extracts column values according to the pattern from a value string. The
	 * keys are {@link Column}s, the values are strings.
	 * @param value value to be checked.
	 * @return a map with <tt>Column</tt> keys and string values
	 * @see de.fuberlin.wiwiss.d2rq.map.ValueSource#getColumnValues(java.lang.String)
	 */
	public Map getColumnValues(String value) {
		if (value == null) {
			return Collections.EMPTY_MAP;
		}
		Matcher match = this.regex.matcher(value);
		if (!match.matches()) {
			return Collections.EMPTY_MAP;
		}
		Map result = new HashMap();
		for (int i = 0; i < this.columns.size(); i++) {
			result.put(this.columns.get(i), match.group(i + 1));
		}
		return result;
	}

	/**
	 * Constructs a String from the pattern using the given database row.
	 * @param row a database row
	 * @return the pattern's value for the given row
	 */
	public String getValue(ResultRow row) {
		int index = 0;
		StringBuffer result = new StringBuffer(this.firstLiteralPart);
		while (index < this.columns.size()) {
			Column column = (Column) this.columns.get(index);
			String value = row.get(column);
			if (value == null) {
				return null;
			}
			result.append(value);				
			result.append(this.literalParts.get(index));
			index++;
		}
		return result.toString();
	}
	
	public String toString() {
		return "Pattern(" + this.pattern + ")";
	}

	public Pattern renameTables(AliasMap renames) {
		int index = 0;
		StringBuffer newPattern = new StringBuffer(this.firstLiteralPart);
		while (index < this.columns.size()) {
			Column column = (Column) this.columns.get(index);
			newPattern.append(DELIMITER);
			newPattern.append(renames.applyTo(column).getQualifiedName());
			newPattern.append(DELIMITER);
			newPattern.append(this.literalParts.get(index));
			index++;
		}
		return new Pattern(newPattern.toString());
	}
	
	private void parsePattern() {
		Matcher match = embeddedColumnRegex.matcher(this.pattern);
		boolean matched = match.find();
		int firstLiteralEnd = matched ? match.start() : this.pattern.length();
		this.firstLiteralPart = this.pattern.substring(0, firstLiteralEnd);
		String regexPattern = "\\Q" + this.firstLiteralPart + "\\E";
		while (matched) {
			this.columns.add(new Column(match.group(1)));
			int nextLiteralStart = match.end();
			matched = match.find();
			int nextLiteralEnd = matched ? match.start() : this.pattern.length();
			String nextLiteralPart = this.pattern.substring(nextLiteralStart, nextLiteralEnd);
			this.literalParts.add(nextLiteralPart);
			regexPattern += "(.*?)\\Q" + nextLiteralPart + "\\E";
		}
		this.regex = java.util.regex.Pattern.compile(regexPattern, java.util.regex.Pattern.DOTALL);
	}
	
	public Iterator partsIterator() {
	    return new Iterator() {
		    private int i = 0;
	        public boolean hasNext() {
	            return i < columns.size() + literalParts.size() + 1;
	        }
	        public Object next() {
	            i++;
	            if (i == 1) {
	            	return firstLiteralPart;
	            } else if (i % 2 == 0) {
	            	return columns.get(i / 2 - 1);
	            }
            	return literalParts.get(i / 2 - 1);
	        }
	        public void remove() {
	        	throw new UnsupportedOperationException();
	        }
	    };
	}
}