package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;


public interface RelationalOperators {

	public final static RelationalOperators DUMMY = new RelationalOperators() {
		public Relation renameColumns(ColumnRenamer renamer) { return null; }
		public Relation select(Expression condition) { return null; }
		public Relation project(Set<? extends ProjectionSpec> projectionSpecs) { return null; }
	};

	/**
	 * <p>Applies the selection operator to this relation.
	 * The new relation will contain only the tuples for which
	 * the expression evaluates to <tt>true</tt>.</p>
	 * 
	 * <p>Selection on attributes that don't exist in the relation are
	 * considered to be always <tt>false</tt> and will cause an empty
	 * relation.</p>
	 * 
	 * @param condition A boolean expression
	 * @return A relation whose tuples satisfy the condition
	 */
	Relation select(Expression condition);
	
	/**
	 * <p>Applies the rename operator to this relation.</p>
	 * 
	 * <p>Renames of attributes that don't exist in the relation are
	 * ignored.</p>
	 * 
	 * @param renamer A map from original to replacement names
	 * @return A relation in which all occurrences of the old
	 * 		names have been replaced with the new ones
	 */
	Relation renameColumns(ColumnRenamer renamer);
	
	/**
	 * <p>Applies the projection operator to this relation.</p>
	 * 
	 * <p>The new relation will contain only the attributes given
	 * as the argument.</p>
	 * 
	 * @param projectionSpecs A set of {@link ProjectionSpec} instances
	 * @return A relation having the specified attributes
	 */
	Relation project(Set<? extends ProjectionSpec> projectionSpecs);
}
