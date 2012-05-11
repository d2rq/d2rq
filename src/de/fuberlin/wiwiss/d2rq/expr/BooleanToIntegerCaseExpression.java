package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * A CASE statement that turns a BOOLEAN (TRUE, FALSE) into an
 * INT (1, 0)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BooleanToIntegerCaseExpression extends Expression {
	private Expression base;
	
	public BooleanToIntegerCaseExpression(Expression base) {
		this.base = base;
	}

	public Expression getBase() {
		return base;
	}
	
	public Set<Attribute> attributes() {
		return base.attributes();
	}

	public boolean isFalse() {
		return base.isFalse();
	}

	public boolean isTrue() {
		return base.isTrue();
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new BooleanToIntegerCaseExpression(base.renameAttributes(columnRenamer));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return "(CASE WHEN (" + base.toSQL(database, aliases) + ") THEN 1 ELSE 0 END)";
	}
	
	public String toString() {
		return "Boolean2Int(" + base + ")";
	}

	public boolean equals(Object other) {
		if (!(other instanceof BooleanToIntegerCaseExpression)) {
			return false;
		}
		BooleanToIntegerCaseExpression otherExpression = (BooleanToIntegerCaseExpression) other;
		return this.base.equals(otherExpression.base); 
	}
	
	public int hashCode() {
		return base.hashCode() ^ 2341234;
	}
}