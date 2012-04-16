package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class UnaryMinus extends Expression {

	private Expression base;
	
	public UnaryMinus(Expression base) {
		this.base = base;
	}
	
	public Expression getBase() {
		return base;
	}

	public Set<Attribute> attributes() {
		return base.attributes();
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new UnaryMinus(base.renameAttributes(columnRenamer));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return "- (" + base.toSQL(database, aliases) + ")";
	}
	
	public String toString() {
		return "- (" + base + ")";
	}

}
