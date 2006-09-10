package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraintWrapper;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Wraps another {@link ValueSource} and presents a view where columns 
 * are renamed according to a {@link ColumnRenamer}.
 * 
 * TODO: Add renameColumns(renamer) and hand calls down the hierarchy? Or flatten hierarchy?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RenamingValueSource.java,v 1.1 2006/09/10 22:18:43 cyganiak Exp $
 */
public class RenamingValueSource implements ValueSource {
	private ValueSource base;
	private ColumnRenamer renames;
	private Set columns;

	public RenamingValueSource(ValueSource base, ColumnRenamer renames) {
		this.base = base;
		this.renames = renames;
		this.columns = this.renames.applyToColumnSet(this.base.getColumns());
	}

	public boolean couldFit(String value) {
		return this.base.couldFit(value);
	}
	
	public Map getColumnValues(String value) {
		return this.renames.applyToMapKeys(this.base.getColumnValues(value));
	}

	public Set getColumns() {
		return this.columns;
	}

	public String getValue(ResultRow row) {
		return this.base.getValue(this.renames.applyTo(row));
	}

	public void matchConstraint(NodeConstraint c) {
		this.base.matchConstraint(new NodeConstraintWrapper(c, this.renames));
	}
}