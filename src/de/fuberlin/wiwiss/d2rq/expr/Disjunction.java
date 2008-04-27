package de.fuberlin.wiwiss.d2rq.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class Disjunction extends Expression {

	public static Expression create(Collection expressions) {
		Set elements = new HashSet(expressions.size());
		Iterator it = expressions.iterator();
		while (it.hasNext()) {
			Expression expression = (Expression) it.next();
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
	
	private Set expressions;
	private Set attributes = new HashSet();
	
	private Disjunction(Set expressions) {
		this.expressions = expressions;
		Iterator it = this.expressions.iterator();
		while (it.hasNext()) {
			Expression expression = (Expression) it.next();
			this.attributes.addAll(expression.attributes());
		}
	}

	public boolean isTrue() {
		return false;
	}

	public boolean isFalse() {
		return false;
	}
	
	public Set attributes() {
		return this.attributes;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		Set renamedExpressions = new HashSet();
		Iterator it = this.expressions.iterator();
		while (it.hasNext()) {
			Expression expression = (Expression) it.next();
			renamedExpressions.add(expression.renameAttributes(columnRenamer));
		}
		return Disjunction.create(renamedExpressions);
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		List fragments = new ArrayList(this.expressions.size());
		Iterator it = this.expressions.iterator();
		while (it.hasNext()) {
			Expression expression = (Expression) it.next();
			fragments.add(expression.toSQL(database, aliases));
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("(");
		it = fragments.iterator();
		while (it.hasNext()) {
			String  fragment = (String ) it.next();
			result.append(fragment);
			if (it.hasNext()) {
				result.append(" OR ");
			}
		}
		result.append(")");
		return result.toString();
	}

	public String toString() {
		List fragments = new ArrayList(this.expressions.size());
		Iterator it = this.expressions.iterator();
		while (it.hasNext()) {
			Expression expression = (Expression) it.next();
			fragments.add(expression.toString());
		}
		Collections.sort(fragments);
		StringBuffer result = new StringBuffer("Disjunction(");
		it = fragments.iterator();
		while (it.hasNext()) {
			String  fragment = (String ) it.next();
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