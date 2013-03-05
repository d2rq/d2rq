package de.fuberlin.wiwiss.d2rq.expr;

import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class TextMatches extends Expression {

	protected final AttributeExpr expr1;
	protected final Expression expr2;

	private final Set<Attribute> columns = new HashSet<Attribute>();

	public TextMatches(AttributeExpr expr1, Expression expr2) {
		this.expr1 = expr1;
		this.expr2 = expr2;
		columns.addAll(expr1.attributes());
		columns.addAll(expr2.attributes());
	}

	public Set<Attribute> attributes() {
		return columns;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public TextMatches renameAttributes(ColumnRenamer columnRenamer) {
		return new TextMatches(expr1.renameAttributes(columnRenamer), expr2.renameAttributes(columnRenamer));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return database.vendor().freeTextExpression(
			expr1.toSQL(database, aliases),
			expr2.toSQL(database, aliases));
	}

	public String toString() {
		return "<TextMatches> (" + expr1 + ", " + expr2 + ")";
	}

	public boolean equals(Object other) {
		if (!(other instanceof TextMatches)) {
			return false;
		}
		TextMatches otherTextMatches = (TextMatches) other;

		return expr1.equals(otherTextMatches.expr1) && expr2.equals(otherTextMatches.expr2);
	}

	public int hashCode() {
		return expr1.hashCode() ^ expr2.hashCode();
	}
}
