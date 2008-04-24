package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO Add uniqueConstraints()
 * TODO Explicitly list tables!!!
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Relation.java,v 1.8 2008/04/24 17:48:52 cyganiak Exp $
 */
public interface Relation extends RelationalOperators {

	static Relation EMPTY = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.FALSE; }
		public Relation select(Expression condition) { return this; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public String toString() { return "Relation.EMPTY"; }
	};
	static Relation TRUE = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.TRUE; }
		// TODO This is dangerous; TRUE can remain TRUE or become EMPTY upon select()
		public Relation select(Expression condition) { return Relation.TRUE; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public String toString() { return "Relation.TRUE"; }
	};
	
	ConnectedDB database();
	
	/**
	 * The tables that are used to set up this relation, both in
	 * their aliased form, and with their original physical names.
	 * @return All table aliases required by this relation
	 */
	AliasMap aliases();
	
	/**
	 * Returns the join conditions that must hold between the tables
	 * in the relation.
	 * @return A set of {@link Join}s 
	 */
	Set joinConditions();

	/**
	 * An expression that must be satisfied for all tuples in the
	 * relation.
	 * @return An expression; {@link Expression#TRUE} indicates no condition
	 */
	Expression condition();
}