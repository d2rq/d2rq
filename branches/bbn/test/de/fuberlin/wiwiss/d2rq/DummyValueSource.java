/*
 * $Id: DummyValueSource.java,v 1.1 2006/04/12 09:56:16 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.List;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.map.ValueSource;

/**
 * Dummy implementation of {@link ValueSource}
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class DummyValueSource implements ValueSource {
	private boolean couldFit = true;
	private String returnValue = null;
	private List columns;
	private Map columnValues;

	public DummyValueSource(String value, boolean couldFit) {
		this.returnValue = value;
		this.couldFit = couldFit;
	}

	public void setCouldFit(boolean couldFit) {
		this.couldFit = couldFit;
	}

	public void setValue(String value) {
		this.returnValue = value;
	}

	public void setColumns(List columns) {
		this.columns = columns;
	}

	public void setColumnValues(Map columnValues) {
		this.columnValues = columnValues;
	}

	public boolean couldFit(String value) {
		return this.couldFit;
	}

	public List getColumns() {
		return this.columns;
	}

	public Map getColumnValues(String value) {
		return this.columnValues;
	}

	public String getValue(String[] row, Map columnNameNumberMap) {
		return this.returnValue;
	}
}
