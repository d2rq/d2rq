/*
 * $Id: Pattern.java,v 1.2 2005/04/13 17:17:42 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * A pattern that combines one or more database columns into a String. Often
 * used as an UriPattern for generating URIs from a column's primary key.
 * <p>
 * (Note: The implementation makes some assumptions about the Column
 * class to keep the code simple and fast. This means Pattern may not
 * work with some hypothetical subclasses of Column.)
 * 
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class Pattern implements ValueSource, Prefixable {
	private String pattern;
	private String firstLiteralPart = null;
	private List columns = new ArrayList(3);
	private List literalParts = new ArrayList(3);
	private Set columnsAsSet;

	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		List old=columns;
		columns=(List)prefixer.prefixCollection(columns);
		if (old!=columns) {
			columnsAsSet=new HashSet(columns);
			pattern=reconstructPattern();
		}
	}
	public void matchPatternIntoNodeConstraint(Pattern other, NodeConstraint c) {
	    boolean startOK=other.firstLiteralPart.startsWith(this.firstLiteralPart) || 
  	  		this.firstLiteralPart.startsWith(other.firstLiteralPart);
	    if (!startOK) {
	        c.possible=false;
	        return;
	    }
	    String thisLastLiteral=(String)literalParts.get(literalParts.size()-1);
	    String otherLastLiteral=(String)other.literalParts.get(other.literalParts.size()-1);
	    boolean endOK=otherLastLiteral.endsWith(thisLastLiteral) || 
	    	thisLastLiteral.endsWith(otherLastLiteral);
	    if (!endOK) {
	        c.possible=false;
	        return;
	    }
	    if ((columns.size()==1) && (other.columns.size()==1) &&
	            this.firstLiteralPart.equals(other.firstLiteralPart) &&
	            thisLastLiteral.equals(otherLastLiteral)) {
	        c.addEqualColumn((Column)columns.get(0),(Column)other.columns.get(0));
	    }
	}

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

	public boolean couldFit(String value) {
		if (value == null) {
			return false;
		}
		if (this.columns.size() == 0) {
			return value.equals(this.firstLiteralPart);
		}
		if (!value.startsWith(this.firstLiteralPart)) {
			return false;
		}
		int index = 0;
		int offset = this.firstLiteralPart.length();
		while (index < this.columns.size() - 1) {
			String literalPart = (String) this.literalParts.get(index);
			offset = value.indexOf(literalPart, offset);
			if (offset == -1) {
				return false;
			}
			offset += literalPart.length();
			index++;
		}
		return value.endsWith((String) this.literalParts.get(index));
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.ValueSource#getColumns()
	 */
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
		Map result = new HashMap();
		if (value == null || this.columns.size() == 0 ||
				!value.startsWith(this.firstLiteralPart)) {
			return result;
		}
		int index = 0;
		int fieldStart = this.firstLiteralPart.length();
		int fieldEnd;
		while (index < this.columns.size() - 1) {
			String literalPart = (String) this.literalParts.get(index);
			fieldEnd = value.indexOf(literalPart, fieldStart);
			if (fieldEnd == -1) {
				return new HashMap();
			}
			result.put(this.columns.get(index), value.substring(fieldStart, fieldEnd));
			fieldStart = fieldEnd + literalPart.length();
			index++;
		}
		String lastLiteralPart = (String) this.literalParts.get(index);
		if (!value.endsWith(lastLiteralPart)) {
			return new HashMap();
		}
		result.put(this.columns.get(index),
				value.substring(fieldStart, value.length() - lastLiteralPart.length()));
		return result;
	}

	/**
	 * Constructs a String from the pattern using the given database row.
	 * @param row a database row
	 * @param columnNameNumberMap a map from qualified column names to indices
	 * 							into the row array
	 * @return the pattern's value for the given row
	 */
	public String getValue(String[] row, Map columnNameNumberMap) {
		int index = 0;
		StringBuffer result = new StringBuffer(this.firstLiteralPart);
		while (index < this.columns.size()) {
			Column column = (Column) this.columns.get(index);
			Integer fieldNumber = (Integer) columnNameNumberMap.get(column.getQualifiedName());
			if (fieldNumber == null) {
				Logger.instance().error("Illegal pattern: '" + this.pattern + "'");
			}
			if (row[fieldNumber.intValue()] == null) {
				return null;
			}
			result.append(row[fieldNumber.intValue()]);				
			result.append(this.literalParts.get(index));
			index++;
		}
		return result.toString();
	}
	
	public String reconstructPattern() {
		StringBuffer result = new StringBuffer(this.firstLiteralPart);
		result.append(D2RQ.deliminator);
		for (int i=0;i<columns.size(); i++) {
			Column column = (Column) columns.get(i);
			result.append(column.getQualifiedName());
			result.append(D2RQ.deliminator);
			result.append(literalParts.get(i));
		}
		return result.toString();
	}
	
	public String toString() {
		return "D2RQ pattern: \"" + this.pattern + "\"";
	}

	private void parsePattern() {
		int fieldStart = this.pattern.indexOf(D2RQ.deliminator);
		if (fieldStart == -1) {
			fieldStart = this.pattern.length();
		}
		// get text before first field
		this.firstLiteralPart = this.pattern.substring(0, fieldStart);
		int fieldEnd;
		while (fieldStart < this.pattern.length()) {
			// find end of field
			fieldStart += D2RQ.deliminator.length();
			fieldEnd = this.pattern.indexOf(D2RQ.deliminator, fieldStart + 1);
			if (fieldEnd == -1) {
				Logger.instance().error("Illegal pattern: '" + this.pattern + "'");
			}
			// get field
			String columnName = this.pattern.substring(fieldStart, fieldEnd).trim();
			this.columns.add(new Column(columnName));
			// find end of text
			fieldEnd += D2RQ.deliminator.length();
			fieldStart = this.pattern.indexOf(D2RQ.deliminator, fieldEnd);
			if (fieldStart == -1) {
				fieldStart = this.pattern.length();
			}
			// get text
			this.literalParts.add(this.pattern.substring(fieldEnd, fieldStart));
		}
	}
}