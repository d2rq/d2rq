package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO Add uniqueConstraints()
 * TODO Explicitly list tables
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Relation.java,v 1.6 2006/11/02 20:46:47 cyganiak Exp $
 */
public interface Relation extends RelationalOperators {

	static Relation EMPTY = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.FALSE; }
		public Map attributeConditions() { return Collections.EMPTY_MAP; }
		public Relation select(Map attributeConditions) { return this; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public String toString() { return "Relation.EMPTY"; }
	};
	static Relation TRUE = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.TRUE; }
		public Map attributeConditions() { return Collections.EMPTY_MAP; }
		public Relation select(Map attributeConditions) { return Relation.EMPTY; }
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
	
	/**
	 * All tuples in the relation must have a certain value for an
	 * attribute if present in this map.
	 * @return A map from {@link Attribute}n to strings
	 */
	Map attributeConditions();
}