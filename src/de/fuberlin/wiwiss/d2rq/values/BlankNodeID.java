package de.fuberlin.wiwiss.d2rq.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
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
 */
public class BlankNodeID implements ValueMaker {
	private final static String DELIMITER = "@@";

	private String classMapID;
	private List<Attribute> attributes;
	
	/**
	 * Constructs a new blank node identifier.
	 * @param classMapID A string that is unique for the class map
	 * 		whose resources are identified by this BlankNodeIdentifier 
	 * @param attributes A set of {@link Attribute}s that uniquely
	 * 		identify the nodes
	 */
	public BlankNodeID(String classMapID, List<Attribute> attributes) {
		this.classMapID = classMapID;
		this.attributes = attributes;
	}

	public List<Attribute> attributes() {
		return this.attributes;
	}
	
	public String classMapID() {
		return this.classMapID;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToBlankNodeID(this);
	}

	public boolean matches(String value) {
		return !valueExpression(value).isFalse();
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
		int i = 1;	// parts[0] is classMap identifier
		Collection<Expression> expressions = new ArrayList<Expression>(attributes.size());
		for (Attribute attribute: attributes) {
			expressions.add(Equality.createAttributeValue(attribute, parts[i]));
			i++;
		}
		return Conjunction.create(expressions);
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return new HashSet<ProjectionSpec>(this.attributes);
	}

	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @return this column's blank node identifier
	 */
	public String makeValue(ResultRow row) {
		StringBuffer result = new StringBuffer(this.classMapID);
		for (Attribute attribute: attributes) {
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
		List<Attribute> replacedAttributes = new ArrayList<Attribute>();
		for (Attribute attribute: attributes) {
			replacedAttributes.add(renamer.applyTo(attribute));
		}
		return new BlankNodeID(this.classMapID, replacedAttributes);
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		List<OrderSpec> result = new ArrayList<OrderSpec>(attributes.size());
		for (Attribute column: attributes) {
			result.add(new OrderSpec(new AttributeExpr(column), ascending));
		}
		return result;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BlankNodeID(");
		Iterator<Attribute> it = attributes.iterator();
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
		List<Expression> parts = new ArrayList<Expression>();
		parts.add(new Constant(classMapID));
		for (Attribute attribute: attributes) {
			parts.add(new Constant(DELIMITER));
			parts.add(new AttributeExpr(attribute));
		}
		return Concatenation.create(parts);
	}
}