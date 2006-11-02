package de.fuberlin.wiwiss.d2rq.expr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;


/**
 * An SQL expression.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Expression.java,v 1.1 2006/11/02 20:46:46 cyganiak Exp $
 */
public abstract class Expression {
	public static final Expression TRUE = new Expression() {
		public Set columns() { return Collections.EMPTY_SET; }
		public boolean isFalse() { return false; }
		public boolean isTrue() { return true; }
		public Expression renameColumns(ColumnRenamer columnRenamer) { return this; }
		public String toSQL(ConnectedDB database, AliasMap aliases) { return "1"; }
		public String toString() { return "TRUE"; }
	};
	public static final Expression FALSE = new Expression() {
		public Set columns() { return Collections.EMPTY_SET; }
		public boolean isFalse() { return true; }
		public boolean isTrue() { return false; }
		public Expression renameColumns(ColumnRenamer columnRenamer) { return this; }
		public String toSQL(ConnectedDB database, AliasMap aliases) { return "0"; }
		public String toString() { return "FALSE"; }
	};

	public abstract boolean isTrue();
	
	public abstract boolean isFalse();
	
	public abstract Set columns();
	
	public abstract Expression renameColumns(ColumnRenamer columnRenamer);
	
	public abstract String toSQL(ConnectedDB database, AliasMap aliases);

	public Expression and(Expression other) {
		List list = new ArrayList(2);
		list.add(this);
		list.add(other);
		return Conjunction.create(list);
	}

	public Expression or(Expression other) {
		List list = new ArrayList(2);
		list.add(this);
		list.add(other);
		return Disjunction.create(list);
	}
}
