package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Restriction which can be chained with another {@link ValueSource} to limit the
 * length of its values. This is useful because the query engine can exclude sources
 * if a value is longer.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MaxLengthRestriction.java,v 1.5 2006/09/09 23:25:15 cyganiak Exp $
 */
public class MaxLengthRestriction implements ValueSource {
	private ValueSource valueSource;
	private int maxLength;
	
	public MaxLengthRestriction(ValueSource valueSource, int maxLength) {
		this.valueSource = valueSource;
		this.maxLength = maxLength;
	}

	public void matchConstraint(NodeConstraint c) {
		this.valueSource.matchConstraint(c);
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

	public String getValue(ResultRow row) {
		return this.valueSource.getValue(row);
	}
}
