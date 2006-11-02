package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class ColumnValue extends Expression {

	public static Expression create(Attribute attribute, String value) {
		return new ColumnValue(attribute, value);
	}
	
	private Attribute attribute;
	private String value;
	
	private ColumnValue(Attribute attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	public Set columns() {
		return Collections.singleton(this.attribute);
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameColumns(ColumnRenamer columnRenamer) {
		return ColumnValue.create(columnRenamer.applyTo(this.attribute), this.value);
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.quoteAttribute(this.attribute) + " = " + 
				database.quoteValue(this.value, aliases.originalOf(this.attribute));
	}
	
	public String toString() {
		return "ColumnValue(" + this.attribute + ", '" + this.value + "')";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof ColumnValue)) {
			return false;
		}
		ColumnValue otherExpression = (ColumnValue) other;
		return this.attribute.equals(otherExpression.attribute) 
				&& this.value.equals(otherExpression.value);
	}
	
	public int hashCode() {
		return this.attribute.hashCode() ^ this.value.hashCode();
	}
}
