package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.map.ValueSource;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Dummy implementation of {@link ValueSource}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DummyValueSource.java,v 1.2 2006/09/09 23:25:14 cyganiak Exp $
 */
public class DummyValueSource implements ValueSource {
	private boolean couldFit = true;
	private String returnValue = null;
	private Set columns;
	private Map columnValues;

	public DummyValueSource(String value, boolean couldFit) {
		this.returnValue = value;
		this.couldFit = couldFit;
	}

	public void matchConstraint(NodeConstraint c) {
	}

	public void setCouldFit(boolean couldFit) {
		this.couldFit = couldFit;
	}

	public void setValue(String value) {
		this.returnValue = value;
	}

	public void setColumns(Set columns) {
		this.columns = columns;
	}

	public void setColumnValues(Map columnValues) {
		this.columnValues = columnValues;
	}

	public boolean couldFit(String value) {
		return this.couldFit;
	}

	public Set getColumns() {
		return this.columns;
	}

	public Map getColumnValues(String value) {
		return this.columnValues;
	}

	public String getValue(ResultRow row) {
		return this.returnValue;
	}
}
