/*
 * $Id: MaxLengthRestriction.java,v 1.3 2005/03/02 09:23:53 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.Map;
import java.util.Set;

/**
 * Restriction which can be chained with another {@link ValueSource} to limit the
 * length of its values. This is useful because the query engine can exclude sources
 * if a value is longer.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class MaxLengthRestriction implements ValueSource, Prefixable {
	private ValueSource valueSource;
	private int maxLength;
	
	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		valueSource=prefixer.prefixValueSource(valueSource);
	}


	public MaxLengthRestriction(ValueSource valueSource, int maxLength) {
		this.valueSource = valueSource;
		this.maxLength = maxLength;
	}

	public boolean couldFit(String value) {
		if (value == null) {
			return true;
		}
		return value.length() <= this.maxLength && this.valueSource.couldFit(value);
	}

	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(String value) {
		return this.valueSource.getColumnValues(value);
	}

	public String getValue(String[] row, Map columnNameNumberMap) {
		return this.valueSource.getValue(row, columnNameNumberMap);
	}
}
