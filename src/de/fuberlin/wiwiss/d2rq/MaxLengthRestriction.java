/*
 * $Id: MaxLengthRestriction.java,v 1.2 2004/08/09 20:16:52 cyganiak Exp $
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
class MaxLengthRestriction implements ValueSource {
	private ValueSource valueSource;
	private int maxLength;

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
