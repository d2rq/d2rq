package de.fuberlin.wiwiss.d2rq.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Concatenation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Constant;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
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
 * @version $Id: BlankNodeID.java,v 1.10 2008/04/27 22:42:38 cyganiak Exp $
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
	
	public String classMapID() {
		return this.classMapID;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToBlankNodeID(this);
	}

	public Expression valueExpression(String value) {
		if (value == null) {
			return Expression.FALSE;
		}
		String[] parts = value.split(DELIMITER);
		// Check if given bNode was created by this class map
		if (parts.length != this.attributes.size() + 1 
				|| !this.classMapID.equals(parts[0])) {
			return Expression.FALSE;		
		}
		Iterator it = this.attributes.iterator();
		int i = 1;	// parts[0] is classMap identifier
		Collection expressions = new ArrayList(attributes.size());
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			expressions.add(Equality.createAttributeValue(attribute, parts[i]));
			i++;
		}
		return Conjunction.create(expressions);
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
	
	public ValueMaker renameAttributes(ColumnRenamer renamer) {
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
	
	public Expression toExpression() {
		List parts = new ArrayList();
		parts.add(new Constant(classMapID));
		Iterator it = attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			parts.add(new Constant(DELIMITER));
			parts.add(new AttributeExpr(attribute));
		}
		return Concatenation.create(parts);
	}
}