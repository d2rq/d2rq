package de.fuberlin.wiwiss.d2rq.values;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Concatenation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Constant;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SQL;

/**
 * A pattern that combines one or more database columns into a String. Often
 * used as an UriPattern for generating URIs from a column's primary key.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Pattern implements ValueMaker {
	public final static String DELIMITER = "@@";
	private final static java.util.regex.Pattern embeddedColumnRegex = 
		java.util.regex.Pattern.compile("@@([^@]+?)(?:\\|(urlencode|urlify|encode))?@@");

	private String pattern;
	private String firstLiteralPart;
	private List columns = new ArrayList(3);
	private List columnFunctions = new ArrayList(3);
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

	public String firstLiteralPart() {
		return firstLiteralPart;
	}
	
	public String lastLiteralPart() {
		if (literalParts.isEmpty()) {
			return firstLiteralPart;
		}
		return (String) literalParts.get(literalParts.size() - 1);
	}
	
	public boolean literalPartsMatchRegex(String regex) {
		if (!this.firstLiteralPart.matches(regex)) {
			return false;
		}
		Iterator it = this.literalParts.iterator();
		while (it.hasNext()) {
			String literalPart = (String) it.next();
			if (!literalPart.matches(regex)) {
				return false;
			}
		}
		return true;
	}
	
	public List attributes() {
		return this.columns;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToPattern(this);
	}

	public Expression valueExpression(String value) {
		if (value == null) {
			return Expression.FALSE;
		}
		Matcher match = this.regex.matcher(value);
		if (!match.matches()) {
			return Expression.FALSE;
		}
		Collection expressions = new ArrayList(columns.size());
		for (int i = 0; i < this.columns.size(); i++) {
			Attribute attribute = (Attribute) this.columns.get(i);
			ColumnFunction function = (ColumnFunction) this.columnFunctions.get(i);
			String attributeValue = function.decode(match.group(i + 1));
			if (attributeValue == null) {
				return Expression.FALSE;
			}
			expressions.add(Equality.createAttributeValue(attribute, attributeValue));
		}
		return Conjunction.create(expressions);
	}

	public Set projectionSpecs() {
		return this.columnsAsSet;
	}

	/**
	 * Constructs a String from the pattern using the given database row.
	 * @param row a database row
	 * @return the pattern's value for the given row
	 */
	public String makeValue(ResultRow row) {
		int index = 0;
		StringBuffer result = new StringBuffer(this.firstLiteralPart);
		while (index < this.columns.size()) {
			Attribute column = (Attribute) this.columns.get(index);
			ColumnFunction function = (ColumnFunction) this.columnFunctions.get(index);
			String value = row.get(column);
			if (value == null) {
				return null;
			}
			value = function.encode(value);
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

	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof Pattern)) {
			return false;
		}
		Pattern other = (Pattern) otherObject;
		return this.pattern.equals(other.pattern);
	}
	
	public int hashCode() {
		return this.pattern.hashCode();
	}
	
	public boolean isEquivalentTo(Pattern p) {
		return this.firstLiteralPart.equals(p.firstLiteralPart)
				&& this.literalParts.equals(p.literalParts);
	}
	
	public ValueMaker renameAttributes(ColumnRenamer renames) {
		int index = 0;
		StringBuffer newPattern = new StringBuffer(this.firstLiteralPart);
		while (index < this.columns.size()) {
			Attribute column = (Attribute) this.columns.get(index);
			ColumnFunction function = (ColumnFunction) this.columnFunctions.get(index); 
			newPattern.append(DELIMITER);
			newPattern.append(renames.applyTo(column).qualifiedName());
			if (function.name() != null) {
				newPattern.append("|");
				newPattern.append(function.name());
			}
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
			this.columns.add(SQL.parseAttribute(match.group(1)));
			this.columnFunctions.add(getColumnFunction(match.group(2)));
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

	public Expression toExpression() {
		List parts = new ArrayList(literalParts.size() * 2 + 1);
		parts.add(new Constant(firstLiteralPart));
		for (int i = 0; i < columns.size(); i++) {
			parts.add(new AttributeExpr((Attribute) columns.get(i)));
			parts.add(new Constant((String) literalParts.get(i)));
		}
		return Concatenation.create(parts);
	}
	
	private final static ColumnFunction IDENTITY = new IdentityFunction();
	private final static ColumnFunction URLENCODE = new URLEncodeFunction();
	private final static ColumnFunction ENCODE = new EncodeFunction();
	private final static ColumnFunction URLIFY = new URLifyFunction();
	
	private ColumnFunction getColumnFunction(String functionName) {
		if ("urlencode".equals(functionName)) {
			return URLENCODE;
		}
		if ("urlify".equals(functionName)) {
			return URLIFY;
		}
		if ("encode".equals(functionName)) {
			return ENCODE;
		}
		if ("".equals(functionName) || functionName == null) {
			return IDENTITY;
		}
		// Shouldn't happen
		throw new D2RQException("Unrecognized column function '" + functionName + "'");
	}
	
	private interface ColumnFunction {
		String encode(String s);
		String decode(String s);
		String name();
	}
	
	static class IdentityFunction implements ColumnFunction {
		public String encode(String s) { return s; }
		public String decode(String s) { return s; }
		public String name() { return null; }
	}
	
	static class URLEncodeFunction implements ColumnFunction {
		public String encode(String s) {
			try {
				return URLEncoder.encode(s, "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			}
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s, "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "urlencode"; }
	}
	static class URLifyFunction implements ColumnFunction {
		public String encode(String s) {
			try {
				return URLEncoder.encode(s, "utf-8")
						.replaceAll("_", "%5F").replace('+', '_');
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			}
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s.replace('_', '+'), "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "urlify"; }
	}
	public static class EncodeFunction implements ColumnFunction {
		private boolean isDigit(int c) {
			return (c >= 48 && c <=57);
		}
		
		private boolean isLetter(int c) {
			return (c >= 65 && c <= 90) || (c >= 97 && c <= 122); 
		}
		
		public String encode(String s) {
			StringBuffer sbuffer = new StringBuffer();
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				int cCode = (int) c;
				
				if (cCode > 128 || c == '-' || c == '_' || c == '~' || c == '.'
						|| isDigit(cCode) || isLetter(cCode)) {
					sbuffer.append(c);
				} else {
					sbuffer.append("%");
					sbuffer.append(Integer.toHexString(cCode).toUpperCase());
				}
			}
			
			return sbuffer.toString();
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s.replaceAll("%20", "+"), "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "encode"; }
	}
}