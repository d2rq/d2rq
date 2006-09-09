package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * NodeMakers represent the nodes of the virtual RDF graph created by
 * D2RQ. One NodeMaker instance describes a set of nodes that are
 * created from a common data source, for example a database column.
 * NodeMakers
 * <ul>
 * <li>can return the database column(s) that the node set is based on
 *   ({@link #getColumns}), along with any necessary join
 *   condtions (@link #getJoins})</li>
 * <li>can check if a concrete node could be in the node set
 *   without querying the DB ({@link #couldFit}),</li>
 * <li>can check what database column
 *   values correspond to a concrete node ({@link #getColumnValues}),</li>
 * <li>build a concrete node from a database result row
 *   ({@link #getNode}),</li>
 * <li>add constraint information to a 
 * {@link NodeConstraint} ({@link #matchConstraint}).</li>
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
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Joerg Garbers
 * @version $Id: NodeMaker.java,v 1.6 2006/09/09 23:25:14 cyganiak Exp $
 */
public interface NodeMaker { 
    
    /** 
     * Adds constraint information to a {@link NodeConstraint}.
     * In a RDQL query a shared variable generally corresponds to different
     * NodeMakers.
     * @param c
     * @see NodeConstraint
     */
    void matchConstraint(NodeConstraint c);
    
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
	 * @return a set of {@link Column}s
	 */
    Set getColumns();

	/**
	 * Returns a set of all join conditions necessary for this
	 * NodeMaker to build its nodes.
	 * @return a set of {@link Join}s 
	 */
    Set getJoins();
	
    /**
     * An SQL expression that must be satisfied for a result row,
     * or the node maker won't build a node out of it.
	 * @return An SQL expression; {@link Expression#TRUE} indicates no condition
     */
    Expression condition();

    /**
     * A set of table aliases that must be used when making an SQL
     * query that will be interpreted by this NodeMaker.
     * @return all table aliases required by this node maker
     */
    AliasMap getAliases();
    
	/**
	 * Creates a new Node from a database result row.
	 * @param row a database result row
	 * @return a node created from the row, or <tt>null</tt> if a <tt>NULL</tt> value
	 *			was encountered in a required field or no node is generated for another reason.
	 */
    Node getNode(ResultRow row);

	/**
	 * Does this NodeMaker produce unique nodes, or may there be duplicates?
	 * @return true iff the NodeMaker is guaranteed to produce unique nodes,
	 * without duplicates
	 */
	boolean isUnique();
}