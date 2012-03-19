package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A {@link ValueMaker} that takes its values from a single
 * column.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Column implements ValueMaker {
	private Attribute attribute;
	private Set attributeAsSet;
	
	public Column(Attribute attribute) {
		this.attribute = attribute;
		this.attributeAsSet = Collections.singleton(this.attribute);
	}
	
	public String makeValue(ResultRow row) {
		return row.get(this.attribute);
	}

	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToAttribute(this.attribute);
	}

	public Expression valueExpression(String value) {
		if (value == null) {
			return Expression.FALSE;
		}
		return Equality.createAttributeValue(attribute, value);
	}

	public Set projectionSpecs() {
		return this.attributeAsSet;
	}

	public ValueMaker renameAttributes(ColumnRenamer renamer) {
		return new Column(renamer.applyTo(this.attribute));
	}
	
	public String toString() {
		return "Column(" + this.attribute.qualifiedName() + ")";
	}
}
