package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * UriMakers transform attribute values from a result set into URIrefs.
 * They are used within TripleMakers.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: UriMaker.java,v 1.7 2006/09/07 15:14:27 cyganiak Exp $
 */
public class UriMaker extends NodeMakerBase {
	private ValueSource valueSource;
	
	public void matchConstraint(NodeConstraint c) {
        c.matchNodeType(NodeConstraint.UriNodeType);
        this.valueSource.matchConstraint(c);
	}       

	public UriMaker(ValueSource valueSource, boolean isUnique) {
		super(isUnique);
		this.valueSource = valueSource;
	}

	public boolean couldFit(Node node) {
		if (Node.ANY.equals(node)) {
			return true;
		}
		return node.isURI() && this.valueSource.couldFit(node.getURI());
	}

	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(Node node) {
		return this.valueSource.getColumnValues(node.getURI());
	}

	public Node getNode(String[] row, Map columnNameNumberMap) {
		String value = this.valueSource.getValue(row, columnNameNumberMap);
		if (value == null) {
			return null;
		}
		return Node.createURI(value);
	}
	
	public String toString() {
		return "URI(" + this.valueSource + ")";
	}
}