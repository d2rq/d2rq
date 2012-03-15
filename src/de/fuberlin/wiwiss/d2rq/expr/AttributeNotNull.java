package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class AttributeNotNull extends Expression {

	public static Expression create(Attribute attribute) {
		return new AttributeNotNull(attribute);
	}
	
	private Attribute attribute;
	
	private AttributeNotNull(Attribute attribute) {
		this.attribute = attribute;
	}
	
	public Set<Attribute> attributes() {
		return Collections.singleton(this.attribute);
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return AttributeNotNull.create(columnRenamer.applyTo(this.attribute));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.getSyntax().quoteAttribute(this.attribute) + " IS NOT NULL";
	}
	
	public String toString() {
		return "AttributeNotNull(" + this.attribute + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof AttributeNotNull)) {
			return false;
		}
		AttributeNotNull otherExpression = (AttributeNotNull) other;
		return this.attribute.equals(otherExpression.attribute); 
	}
	
	public int hashCode() {
		return this.attribute.hashCode();
	}
}
