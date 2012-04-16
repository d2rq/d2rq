package de.fuberlin.wiwiss.d2rq.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class Disjunction extends Expression {

	public static Expression create(Collection<Expression> expressions) {
		Set<Expression> elements = new HashSet<Expression>(expressions.size());
		for (Expression expression: expressions) {
			if (expression.isTrue()) {
				return Expression.TRUE;
			}
			if (expression.isFalse()) {
				continue;
			}
			if (expression instanceof Disjunction) {
				elements.addAll(((Disjunction) expression).expressions);
			} else {
				elements.add(expression);
			}
		}
		if (elements.isEmpty()) {
			return Expression.FALSE;
		}
		if (elements.size() == 1) {
			return (Expression) elements.iterator().next();
		}
		return new Disjunction(elements);
	}
	
	private Set<Expression> expressions;
	private Set<Attribute> attributes = new HashSet<Attribute>();
	
	private Disjunction(Set<Expression> expressions) {
		this.expressions = expressions;
		for (Expression expression: expressions) {
			this.attributes.addAll(expression.attributes());
		}
	}

	public boolean isTrue() {
		return false;
	}

	public boolean isFalse() {
		return false;
	}
	
	public Set<Attribute> attributes() {
		return this.attributes;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		Set<Expression> renamedExpressions = new HashSet<Expression>();
		for (Expression expression: expressions) {
			renamedExpressions.add(expression.renameAttributes(columnRenamer));
		}
		return Disjunction.create(renamedExpressions);
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		List<String> fragments = new ArrayList<String>(expressions.size());
		for (Expression expression: expressions) {
			fragments.add(expression.toSQL(database, aliases));
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("(");
		Iterator<String> it = fragments.iterator();
		while (it.hasNext()) {
			String fragment = it.next();
			result.append(fragment);
			if (it.hasNext()) {
				result.append(" OR ");
			}
		}
		result.append(")");
		return result.toString();
	}

	public String toString() {
		List<String> fragments = new ArrayList<String>(expressions.size());
		for (Expression expression: expressions) {
			fragments.add(expression.toString());
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("Disjunction(");
		Iterator<String> it = fragments.iterator();
		while (it.hasNext()) {
			String fragment = it.next();
			result.append(fragment);
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Disjunction)) {
			return false;
		}
		Disjunction otherConjunction = (Disjunction) other;
		return this.expressions.equals(otherConjunction.expressions);
	}
	
	public int hashCode() {
		return this.expressions.hashCode();
	}
}