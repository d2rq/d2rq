package de.fuberlin.wiwiss.d2rq.values;

import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Describes a set of strings that are obtained in some way
 * from one or more database columns.
 * <p>
 * Typical implementations are {@link Attribute} (describing the set
 * of strings contained in one database column), {@link Pattern}
 * (describing the set of strings that is obtained by sticking
 * the values of several database fields into a string pattern),
 * and {@link BlankNodeID} (similar).
 * <p>
 * There are several other ValueSources that modify the behaviour
 * of another underlying ValueSource, implementing the Decorator
 * pattern. This includes {@link TranslationTable}.TranslatingValueSource
 * (translates values using a translation table or translation class)
 * and the various value restrictions (@link RegexRestriction et. al.).
 * <p>
 * ValueSources are used by <tt>NodeMaker</tt>s. A node maker
 * wraps the strings into Jena nodes, thus creating a description
 * of a set of RDF nodes.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ValueMaker.java,v 1.7 2007/11/05 22:59:38 cyganiak Exp $
 */
public interface ValueMaker {
    
	/**
	 * Checks if a given value fits this source without querying the
	 * database. A value maker should never match <tt>null</tt>.
	 */
	boolean matches(String value);

	/**
	 * Returns a map of database fields and values corresponding
	 * to the argument.
	 * 
	 * <p>For example, a ValueSource that corresponds directly
	 * to a single DB column would return a single-entry map with that
	 * column as the key, and value as the value.
	 * 
	 * <p>The result is undefined if {@link #matches(String)} is false
	 * for the same value.
	 * 
	 * @param value a non-<tt>null</tt> value
	 * @return a map with {@link Attribute} keys, and string values
	 */
	Map attributeConditions(String value);

	/**
	 * Returns a set of all columns containing data necessary
	 * for this ValueSource.
	 * @return a set of {Column}s
	 */
	Set projectionAttributes();

	/**
	 * Retrieves a value from a database row according to some rule or pattern.
	 * @param row the database row
	 * @return a value created from the row
	 */
	String makeValue(ResultRow row);
	
	void describeSelf(NodeSetFilter c);

	ValueMaker replaceColumns(ColumnRenamer renamer);
}
