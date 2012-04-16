package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class AttributeExpr extends Expression {
	private final Attribute attribute;
	
	public AttributeExpr(Attribute attribute) {
		this.attribute = attribute;
	}
	
	public Set<Attribute> attributes() {
		return Collections.singleton(attribute);
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new AttributeExpr(columnRenamer.applyTo(attribute));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.vendor().quoteAttribute(attribute);
	}

	public String toString() {
		return "AttributeExpr(" + attribute + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof AttributeExpr)) {
			return false;
		}
		return attribute.equals(((AttributeExpr) other).attribute);
	}
	
	public int hashCode() {
		return this.attribute.hashCode();
	}
}
