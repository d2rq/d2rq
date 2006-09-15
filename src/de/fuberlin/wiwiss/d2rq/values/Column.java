package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A {@link ValueMaker} that takes its values from a single
 * column.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Column.java,v 1.3 2006/09/15 15:31:22 cyganiak Exp $
 */
public class Column implements ValueMaker {
	private Attribute attribute;
	private Set attributeAsSet;
	
	public Column(Attribute attribute) {
		this.attribute = attribute;
		this.attributeAsSet = Collections.singleton(this.attribute);
	}
	
	public Map attributeConditions(String value) {
		return Collections.singletonMap(this.attribute, value);
	}

	public String makeValue(ResultRow row) {
		return row.get(this.attribute);
	}

	public void matchConstraint(NodeConstraint c) {
		c.matchColumn(this.attribute);
	}

	public boolean matches(String value) {
		return true;
	}

	public Set projectionAttributes() {
		return this.attributeAsSet;
	}

	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		return new Column(renamer.applyTo(this.attribute));
	}
	
	public String toString() {
		return "Column(" + this.attribute.qualifiedName() + ")";
	}
}
