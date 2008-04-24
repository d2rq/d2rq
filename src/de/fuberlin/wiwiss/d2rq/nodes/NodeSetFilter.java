package de.fuberlin.wiwiss.d2rq.nodes;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * Defines constraints to a set of RDF {@link Node}s.
 * 
 * TODO: Should there be limitValuesToExpression(Expression)?
 * 
 * @author Richard Cyganiak
 * @version $Id: NodeSetFilter.java,v 1.2 2008/04/24 17:48:53 cyganiak Exp $
 */
public interface NodeSetFilter {

	/**
	 * Limits the node set to the empty set.
	 */
	void limitToEmptySet();

	/** 
	 * Limits this node set to one particular node.
	 * @param node A fixed singleton node
	 */
	void limitTo(Node node);

	/**
	 * Limits this node set to URI nodes.
	 */
	void limitToURIs();

	/**
	 * Limits this node set to blank nodes.
	 */
	void limitToBlankNodes();

	/**
	 * Limits this node set to literals having a particular language
	 * tag and datatype.
	 * @param language The language tag of all nodes in the set, or <tt>null</tt>
	 * 		for plain or datatype literals
	 * @param datatype The datatype of all nodes in the set, or <tt>null</tt>
	 * 		for plain literals
	 */
	public void limitToLiterals(String language, RDFDatatype datatype);

	/**
	 * Limits this node set to those whose value matches a value in a 
	 * particular database table column.
	 * @param attribute The attribute containing possible values
	 */
	public void limitValuesToAttribute(Attribute attribute);

	/**
	 * Limits this node set to nodes whose value matches the values
	 * produced by a pattern.
	 * @param pattern The pattern producing possible values
	 */
	public void limitValuesToPattern(Pattern pattern);

	/**
	 * Limits this node set to nodes whose value matches the values
	 * produced by a blank node ID.
	 * @param id The blank node ID producing possible values
	 */
	public void limitValuesToBlankNodeID(BlankNodeID id);
}