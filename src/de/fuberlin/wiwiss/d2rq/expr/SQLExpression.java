package de.fuberlin.wiwiss.d2rq.expr;

import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.SQL;


/**
 * An SQL expression.
 * 
 * TODO: Shouldn't call to SQL so much
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLExpression extends Expression {
	
	public static Expression create(String sql) {
		sql = sql.trim();
		if ("1".equals(sql)) {
			return Expression.TRUE;
		}
		if ("0".equals(sql)) {
			return Expression.FALSE;
		}
		return new SQLExpression(sql);
	}
	
	private String expression;
	private Set<Attribute> columns = new HashSet<Attribute>();
	
	private SQLExpression(String expression) {
		this.expression = expression;
		this.columns = SQL.findColumnsInExpression(this.expression);
	}

	public boolean isTrue() {
		return false;
	}
	
	public boolean isFalse() {
		return false;
	}
	
	public Set<Attribute> attributes() {
		return this.columns;
	}
	
	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		return new SQLExpression(SQL.replaceColumnsInExpression(this.expression, columnRenamer));
	}
	
	public String toSQL(ConnectedDB database, AliasMap aliases) {
		return "(" + SQL.quoteColumnsInExpression(this.expression, database) + ")";
	}
	
	public String toString() {
		return "SQL(" + this.expression + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof SQLExpression)) {
			return false;
		}
		SQLExpression otherExpression = (SQLExpression) other;
		return this.expression.equals(otherExpression.expression);
	}
	
	public int hashCode() {
		return this.expression.hashCode();
	}

	public String getExpression() 
	{
		return expression;
	}
	
	
}
