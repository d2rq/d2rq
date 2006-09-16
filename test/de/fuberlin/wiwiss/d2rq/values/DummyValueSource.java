package de.fuberlin.wiwiss.d2rq.values;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

/**
 * Dummy implementation of {@link ValueMaker}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DummyValueSource.java,v 1.2 2006/09/16 14:19:20 cyganiak Exp $
 */
public class DummyValueSource implements ValueMaker {
	private boolean couldFit = true;
	private String returnValue = null;
	private Set columns;
	private Map columnValues;

	public DummyValueSource(String value, boolean couldFit) {
		this.returnValue = value;
		this.couldFit = couldFit;
	}

	public void describeSelf(NodeSetFilter c) {
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
	
	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		return this;
	}
}
