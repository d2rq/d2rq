package de.fuberlin.wiwiss.d2rq.values;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
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
 * @version $Id: ValueMaker.java,v 1.10 2008/04/25 16:27:41 cyganiak Exp $
 */
public interface ValueMaker {
    
	Expression valueExpression(String value);

	/**
	 * Returns a set of all {@link ProjectionSpec}s containing data necessary
	 * for this ValueSource.
	 * @return a set of {@link ProjectionSpec}s
	 */
	Set projectionSpecs();

	/**
	 * Retrieves a value from a database row according to some rule or pattern.
	 * @param row the database row
	 * @return a value created from the row
	 */
	String makeValue(ResultRow row);
	
	void describeSelf(NodeSetFilter c);

	ValueMaker renameAttributes(ColumnRenamer renamer);
}
