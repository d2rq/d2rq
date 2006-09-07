package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * BlankNodeMakers transform attribute values from a result set into blank nodes.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: BlankNodeMaker.java,v 1.7 2006/09/07 15:14:27 cyganiak Exp $
 */
public class BlankNodeMaker extends NodeMakerBase {
	private ValueSource valueSource;
	
	public void matchConstraint(NodeConstraint c) {
		c.matchNodeType(NodeConstraint.BlankNodeType);
		this.valueSource.matchConstraint(c);
	}       
	
	public BlankNodeMaker(ValueSource valueSource, boolean isUnique) {
		super(isUnique);
		this.valueSource = valueSource;
	}

	public boolean couldFit(Node node) {
		if (Node.ANY.equals(node)) {
			return true;
		}
		return node.isBlank() &&
				this.valueSource.couldFit(node.getBlankNodeId().toString());
	}

	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(Node node) {
		return this.valueSource.getColumnValues(node.getBlankNodeId().toString());
	}

	/**
	 * Creates a new blank node based on the current row of the result set
	 * and the mapping of database column names to elements of the array.
	 * Returns null if a NULL value was retrieved from the database.
	 */
	public Node getNode(String[] row, Map columnNameNumberMap) {
		String value = this.valueSource.getValue(row, columnNameNumberMap);
		if (value == null) {
			return null;
		}		
		return Node.createAnon(new AnonId(value));
	}
	
	public String toString() {
		return "Blank(" + this.valueSource + ")";
	}
}