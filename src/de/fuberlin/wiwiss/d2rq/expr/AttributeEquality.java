package de.fuberlin.wiwiss.d2rq.expr;

import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class AttributeEquality extends Expression {

	public static Expression create(Attribute attribute1, Attribute attribute2) {
		if (attribute1.equals(attribute2)) {
			return Expression.TRUE;
		}
		return (attribute1.compareTo(attribute2) < 0)
				? new AttributeEquality(attribute1, attribute2)
				: new AttributeEquality(attribute2, attribute1);
	}
	
	private Attribute attribute1;
	private Attribute attribute2;
	private Set attributes = new HashSet();
	
	// arguments MUST be ordered!
	private AttributeEquality(Attribute attribute1, Attribute attribute2) {
		this.attribute1 = attribute1;
		this.attribute2 = attribute2;
		attributes.add(attribute1);
		attributes.add(attribute2);
	}
	
	public Set columns() {
		return this.attributes;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameColumns(ColumnRenamer columnRenamer) {
		return AttributeEquality.create(
				columnRenamer.applyTo(this.attribute1), 
				columnRenamer.applyTo(this.attribute2));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.quoteAttribute(this.attribute1) + " = " + database.quoteAttribute(this.attribute2);
	}
	
	public String toString() {
		return "ColumnEquality(" + this.attribute1 + ", " + this.attribute2 + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof AttributeEquality)) {
			return false;
		}
		AttributeEquality otherExpression = (AttributeEquality) other;
		return this.attributes.equals(otherExpression.attributes);
	}
	
	public int hashCode() {
		return this.attributes.hashCode();
	}
}
