package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.values.ValueSource;

/**
 * Dummy implementation of {@link ValueSource}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DummyValueSource.java,v 1.3 2006/09/11 22:29:21 cyganiak Exp $
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

	public boolean matches(String value) {
		return this.couldFit;
	}

	public Set projectionAttributes() {
		return this.columns;
	}

	public Map attributeConditions(String value) {
		return this.columnValues;
	}

	public String makeValue(ResultRow row) {
		return this.returnValue;
	}
	
	public ValueSource replaceColumns(ColumnRenamer renamer) {
		return this;
	}
}
