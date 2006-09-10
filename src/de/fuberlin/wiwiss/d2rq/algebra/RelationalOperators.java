package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Map;

import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;

public interface RelationalOperators {

	/**
	 * <p>Applies the selection operator to this relation, using equality
	 * conditions on a number of attributes as the selection expression.
	 * The new relation will contain only tuples whose attribute values
	 * are equal to the string values in the argument map.</p>
	 * 
	 * <p>Selection on attributes that don't exist in the relation are
	 * considered to be always <tt>false</tt> and will cause an empty
	 * relation.</p>
	 * 
	 * @param attributeConditions A map from {@link Column}s to Strings
	 * @return A relation whose tuples satisfy the conditions
	 */
	Relation select(Map attributeConditions);
	
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
}
