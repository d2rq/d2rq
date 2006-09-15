package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;


/**
 * An SQL expression.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Expression.java,v 1.4 2006/09/15 12:25:25 cyganiak Exp $
 */
public class Expression {
	public static final Expression TRUE = new Expression("1");
	public static final Expression FALSE = new Expression("0");

	private String expression;
	private Set columns = new HashSet();
	
	public Expression(String expression) {
		this(Collections.singletonList(expression));
	}
	
	public Expression(List expressions) {
		if (expressions.isEmpty()) {
			this.expression = "1";
		} else {
			this.expression = "";
			Iterator it = expressions.iterator();
			while (it.hasNext()) {
				String nextExpression = (String) it.next();
				this.expression += nextExpression;
				if (it.hasNext()) {
					this.expression += " AND ";
				}
			}
		}
		this.columns = Attribute.findColumnsInExpression(this.expression);
	}

	public boolean isTrue() {
		return this.expression.equals("1");
	}
	
	public Set columns() {
		return this.columns;
	}
	
	public Expression renameColumns(ColumnRenamer columnRenamer) {
		return new Expression(Attribute.replaceColumnsInExpression(this.expression, columnRenamer));
	}
	
	public Expression and(Expression other) {
		if (isTrue()) {
			return other;
		}
		if (other.isTrue()) {
			return this;
		}
		return new Expression(this.expression + " AND " + other.expression);
	}
	
	public String toSQL(ConnectedDB database) {
		return Attribute.quoteColumnsInExpression(this.expression, database);
	}
	
	public String toString() {
		return "Expression(" + this.expression + ")";
	}
	
	/**
	 * An expression equals another expression iff their SQL strings are identical.
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Expression)) {
			return false;
		}
		Expression otherExpression = (Expression) other;
		return this.expression.equals(otherExpression.expression);
	}
	
	public int hashCode() {
		return this.expression.hashCode();
	}
}
