 /*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

/**
 * NodeMakers create Nodes from database result rows. The actual mapping from
 * the database result to a value happens within a {@link ValueSource}. A
 * NodeMaker is responsible for creating a Node instance from that value.
 *
 * A node can be created from:
 * 
 * 1. A fixed value (URI/Literal)
 * 2. The value of a column in the database (URI/Literal)
 * 3. A pattern which includes one or more columns from the database (URI/Literal)
 * 4. One or more columns values that are used for the bNodeID (bNode)
 * 
 * <BR>History: 06-16-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 * @see de.fuberlin.wiwiss.d2rq.UriMaker
 * @see de.fuberlin.wiwiss.d2rq.LiteralMaker
 * @see de.fuberlin.wiwiss.d2rq.BlankNodeMaker
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