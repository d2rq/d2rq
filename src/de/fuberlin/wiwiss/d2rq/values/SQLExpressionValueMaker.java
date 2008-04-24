package de.fuberlin.wiwiss.d2rq.values;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A value maker that creates its values from a SQL expression.
 * 
 * TODO Write unit tests
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SQLExpressionValueMaker.java,v 1.1 2008/04/24 17:48:53 cyganiak Exp $
 */
public class SQLExpressionValueMaker implements ValueMaker, ProjectionSpec {
	private final Expression expression;
	private final String name;
	
	public SQLExpressionValueMaker(Expression expression) {
		this.expression = expression;
		this.name = "expr" + Integer.toHexString(expression.hashCode());
	}
	
	public void describeSelf(NodeSetFilter c) {
		// Do nothing, the value of a SQL expression could be anything
		// TODO: Should there be c.limitToExpression(...)?
	}
	
	public Set projectionSpecs() {
		return Collections.singleton(this);
	}
	
	public Map attributeConditions(String value) {
		return Collections.singletonMap(this, value);
	}

	public String makeValue(ResultRow row) {
		return row.get(this);
	}

	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		return new SQLExpressionValueMaker(renamer.applyTo(expression));
	}
	
	public void selectValue(String value, MutableRelation relation) {
		relation.select(Equality.createExpressionValue(expression, value));
	}
	
	public Expression toExpression() {
		return expression;
	}
	
	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return expression.toSQL(database, aliases) + " AS " + name;
	}
	
	public Set requiredAttributes() {
		return expression.columns();
	}
	
	public int hashCode() {
		return expression.hashCode();
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof SQLExpressionValueMaker)) {
			return false;
		}
		return expression.equals(((SQLExpressionValueMaker) other).expression);
	}
	
	public String toString() {
		return "SQLExpression(" + expression + " AS " + name + ")";
	}
}
