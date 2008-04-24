package de.fuberlin.wiwiss.d2rq.values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * A blank node identifier that uniquely identifies all resources generated from
 * a specific ClassMap.
 * <p>
 * (Note: The implementation makes some assumptions about the Column
 * class to keep the code simple and fast. This means BlankNodeIdentifier
 * might not work with some hypothetical subclasses of Column.)
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: BlankNodeID.java,v 1.7 2008/04/24 17:48:53 cyganiak Exp $
 */
public class BlankNodeID implements ValueMaker {
	private final static String DELIMITER = "@@";

	private String classMapID;
	private List attributes;
	
	/**
	 * Constructs a new blank node identifier.
	 * @param classMapID A string that is unique for the class map
	 * 		whose resources are identified by this BlankNodeIdentifier 
	 * @param attributes A set of {@link Attribute}s that uniquely
	 * 		identify the nodes
	 */
	public BlankNodeID(String classMapID, List attributes) {
		this.classMapID = classMapID;
		this.attributes = attributes;
	}

	public List attributes() {
		return this.attributes;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToBlankNodeID(this);
	}

	public void selectValue(String value, MutableRelation relation) {
		if (value == null) {
			relation.empty();
			return;
		}
		String[] parts = value.split(DELIMITER);
		// Check if given bNode was created by this class map
		if (parts.length != this.attributes.size() + 1 
				|| !this.classMapID.equals(parts[0])) {
			relation.empty();
			return;		
		}
		Iterator it = this.attributes.iterator();
		int i = 1;	// parts[0] is classMap identifier
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			relation.select(Equality.createAttributeValue(attribute, parts[i]));
			i++;
		}
	}

	public Set projectionSpecs() {
		return new HashSet(this.attributes);
	}

	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @return this column's blank node identifier
	 */
	public String makeValue(ResultRow row) {
		StringBuffer result = new StringBuffer(this.classMapID);
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			String value = row.get(attribute);
			if (value == null) {
				return null;
		    }
			result.append(DELIMITER);
			result.append(value);
		}
        return result.toString();
	}
	
	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		List replacedAttributes = new ArrayList();
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			replacedAttributes.add(renamer.applyTo(attribute));
		}
		return new BlankNodeID(this.classMapID, replacedAttributes);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BlankNodeID(");
		Iterator it = this.attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.append(attribute.qualifiedName());
			if (it.hasNext()) {
				result.append(",");
			}
		}
		result.append(")");
		return result.toString();
	}
}