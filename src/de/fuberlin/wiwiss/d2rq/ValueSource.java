/*
 * $Id: ValueSource.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.Map;
import java.util.Set;

/**
 * Represents a source of strings that are in some way created from a
 * database record. Typically used by a {@link NodeMaker}
 * to retrieve node values.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
interface ValueSource {

	/**
	 * Checks if a given value fits this source without querying the
	 * database.
	 */
	boolean couldFit(String value);

	/**
	 * Returns a map of database fields and values corresponding
	 * to the argument.
	 * 
	 * <p>For example, a ValueSource that corresponds directly
	 * to a single DB column would return a single-entry map with that
	 * column as the key, and value as the value.
	 * 
	 * @param value a non-<tt>null</tt> value
	 * @return a map with {@link Column} keys, and string values.
	 */
	Map getColumnValues(String value);

	/**
	 * Returns a set of all columns containing data necessary
	 * for this ValueSource.
	 * @return a set of {Column}s
	 */
	Set getColumns();

	/**
	 * Retrieves a value from a database row according to some rule or pattern.
	 * @param row the database row
	 * @param columnNameNumberMap a map from <tt>Table.Column</tt> style column
	 * 							names to Integer indices into the row array
	 * @return a value created from the row
	 */
	String getValue(String[] row, Map columnNameNumberMap);
}
