 /*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

/**
 * NodeMakers represent the nodes of the virtual RDF graph created by
 * D2RQ. One NodeMaker instance describes a set of nodes that are
 * created from a common data source, for example a database column.
 * NodeMakers
 * <ul>
 * <li>can return the database column(s) that the node set is based on
 *   ({@link #getColumns}),</li>
 * <li>can check if a concrete node could be in the node set
 *   without querying the DB ({@link #couldFit}),</li>
 * <li>can check what database column
 *   values correspond to a concrete node ({@link #getColumnValues}),</li>
 * <li>and build a concrete node from a database result row
 *   ({@link #getNode}).</li>
 * </ul>
 * <p>
 * Most of the actual work is done by a chain of {@link ValueSource}
 * instances that lie below the NodeMaker. The NodeMaker only wraps
 * and unwraps the String values (URIs, literals and blank node IDs)
 * into/from Jena Node instances.
 * </p>
 * NodeMakers are used by {@link PropertyBridge}s as their subjects,
 * predicates and objects.
 * </p>
 * There are implementations for the different types of RDF nodes:
 * {@link UriMaker}, {@link LiteralMaker} and {@link BlankNodeMaker}.
 * A special implementation is the {@link FixedNodeMaker}, which
 * is a single-element node set.
 * <p>
 * TODO: Better name for NodeMaker: NodeSetDescription?
 * 
 * <p>History:<br>
 * 06-16-2004: Initial version of this class.
 * 08-04-2004: Added couldFit, getColumns and getColumnValues
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
interface NodeMaker {

	/**
	 * Checks if a node could fit this NodeMaker without querying the
	 * database.
	 */
	boolean couldFit(Node node);

	/**
	 * Returns a map of database fields and values corresponding
	 * to the argument node.
	 *  
	 * <p>For example, a NodeMaker that corresponds directly
	 * to a single DB column would return a single-entry map with that
	 * column as the key, and the DB value corresponding to the
	 * node as the value.
	 * @param node a concrete, non-<tt>null</tt> RDF node 
	 * @return a map with {@link Column} keys, and string values.
	 */
	Map getColumnValues(Node node);

	/**
	 * Returns a set of all columns containing data necessary
	 * for this NodeMaker to build its nodes.
	 * @return a set of {Column}s
	 */
	Set getColumns();

	/**
	 * Creates a new Node from a database result row.
	 * @param row a database result row
	 * @param columnNameNumberMap a map from <tt>Table.Column</tt> style column names
	 * 							to Integers representing indices within the row array
	 * @return a node created from the row, or <tt>null</tt> if a <tt>NULL</tt> value
	 *			was encountered in a required field.
	 */
	Node getNode(String[] row, Map columnNameNumberMap);
}