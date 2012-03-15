package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

public class NodeSetFilterImpl implements NodeSetFilter {
	private final static int NODE_TYPE_UNKNOWN = 0;
	private final static int NODE_TYPE_URI = 1;
	private final static int NODE_TYPE_LITERAL = 2;
	private final static int NODE_TYPE_BLANK = 3;
	
	private boolean isEmpty = false;
	private int type = NODE_TYPE_UNKNOWN;
	private String constantValue = null;
	private String constantLanguage = null;
	private RDFDatatype constantDatatype = null;
	private Node fixedNode = null;
	private Collection<Attribute> attributes = new HashSet<Attribute>();
	private Collection<Pattern> patterns = new HashSet<Pattern>();
	private Collection<Expression> expressions = new HashSet<Expression>();
	private Collection<BlankNodeID> blankNodeIDs = new HashSet<BlankNodeID>();
	private String valueStart = null;
	private String valueEnd = null;
	
	public void limitToEmptySet() {
		isEmpty = true;
	}
	
	public void limitToURIs() {
		limitToNodeType(NODE_TYPE_URI);
	}
	
	public void limitToBlankNodes() {
		limitToNodeType(NODE_TYPE_BLANK);
	}

	public void limitToLiterals(String language, RDFDatatype datatype) {
		limitToNodeType(NODE_TYPE_LITERAL);
		if (constantLanguage == null) {
			constantLanguage = language;
		} else {
			if (!constantLanguage.equals(language)) {
				limitToEmptySet();
			}
		}
		if (constantDatatype == null) {
			constantDatatype = datatype;
		} else {
			if (!constantDatatype.equals(datatype)) {
				limitToEmptySet();
			}
		}
	}

	private void limitToNodeType(int limitType) {
		if (type == NODE_TYPE_UNKNOWN) {
			type = limitType;
			return;
		}
		if (type == limitType) {
			return;
		}
		limitToEmptySet();
	}
	
	public void limitTo(Node node) {
		if (Node.ANY.equals(node) || node.isVariable()) {
			return;
		}
		if (fixedNode == null) {
			fixedNode = node;
		} else if (!fixedNode.equals(node)) {
			limitToEmptySet();
		}
		if (node.isURI()) {
			limitToURIs();
			limitValues(node.getURI());
		}
		if (node.isBlank()) {
			limitToBlankNodes();
			limitValues(node.getBlankNodeLabel());
		}
		if (node.isLiteral()) {
			limitToLiterals(node.getLiteralLanguage(), node.getLiteralDatatype());
			limitValues(node.getLiteralLexicalForm());
		}
	}
	
	public void limitValues(String constant) {
		if (constantValue == null) {
			constantValue = constant;
			return;
		}
		if (!constantValue.equals(constant)) {
			limitToEmptySet();
		}
	}

	public void limitValuesToAttribute(Attribute attribute) {
		attributes.add(attribute);
	}

	public void limitValuesToBlankNodeID(BlankNodeID id) {
		if (!blankNodeIDs.isEmpty()) {
			BlankNodeID first = (BlankNodeID) blankNodeIDs.iterator().next();
			if (!first.classMapID().equals(id.classMapID())) {
				limitToEmptySet();
				return;
			}
		}
		blankNodeIDs.add(id);
	}

	public void limitValuesToPattern(Pattern pattern) {
		patterns.add(pattern);
		if (valueStart == null || pattern.firstLiteralPart().startsWith(valueStart)) {
			valueStart = pattern.firstLiteralPart();
		} else if (!valueStart.startsWith(pattern.firstLiteralPart())) {
			limitToEmptySet();
		}
		if (valueEnd == null || pattern.lastLiteralPart().endsWith(valueEnd)) {
			valueEnd = pattern.lastLiteralPart();
		} else if (!valueEnd.endsWith(pattern.lastLiteralPart())) {
			limitToEmptySet();
		}
	}

	public void limitValuesToExpression(Expression expression) {
		expressions.add(expression);
	}

	public boolean isEmpty() {
		if (isEmpty) return true;
		if (!blankNodeIDs.isEmpty() && fixedNode != null) {
			BlankNodeID first = (BlankNodeID) blankNodeIDs.iterator().next();
			if (Expression.FALSE.equals(
					first.valueExpression(fixedNode.getBlankNodeLabel()))) {
				return true;
			}
		}
		if (!patterns.isEmpty()) {
			for (Pattern pattern: patterns) {
				if (constantValue != null 
						&& Expression.FALSE.equals(pattern.valueExpression(constantValue))) {
					return true;
				}
			}
		}
		return false;
	}
	
	private List<Expression> matchPatterns(Pattern p1, Pattern p2) {
		List<Expression> results = new ArrayList<Expression>(p1.attributes().size());
		if (p1.isEquivalentTo(p2)) {
			for (int i = 0; i < p1.attributes().size(); i++) {
				Attribute col1 = p1.attributes().get(i);
				Attribute col2 = p2.attributes().get(i);
				results.add(Equality.createAttributeEquality(col1, col2));
			}
		} else {
			results.add(Equality.create(p1.toExpression(), p2.toExpression()));
			limitToEmptySet();
		}
		return results;
	}
	
	private List<Expression> matchBlankNodeIDs(BlankNodeID id1, BlankNodeID id2) {
		List<Expression> results = new ArrayList<Expression>(id1.attributes().size());
		for (int i = 0; i < id1.attributes().size(); i++) {
			Attribute col1 = id1.attributes().get(i);
			Attribute col2 = id2.attributes().get(i);
			results.add(Equality.createAttributeEquality(col1, col2));
		}
		return results;
	}
	
	public Expression constraint() {
		if (isEmpty()) {
			return Expression.FALSE;
		}
		List<Expression> translated = new ArrayList<Expression>();
		if (attributes.size() >= 2) {
			Iterator<Attribute> it = attributes.iterator();
			Attribute first = it.next();
			while (it.hasNext()) {
				translated.add(Equality.createAttributeEquality(first, it.next()));
			}
		}
		if (patterns.size() >= 2) {
			Iterator<Pattern> it = patterns.iterator();
			Pattern first = it.next();
			while (it.hasNext()) {
				translated.addAll(matchPatterns(first, it.next()));
			}
		}
		if (expressions.size() >= 2) {
			Iterator<Expression> it = expressions.iterator();
			Expression first = it.next();
			while (it.hasNext()) {
				translated.add(Equality.create(first, it.next()));
			}
		}
		if (blankNodeIDs.size() >= 2) {
			Iterator<BlankNodeID> it = blankNodeIDs.iterator();
			BlankNodeID first = it.next();
			while (it.hasNext()) {
				translated.addAll(matchBlankNodeIDs(first, it.next()));
			}
		}
		if (constantValue != null) {
			if (!attributes.isEmpty()) {
				Attribute first = attributes.iterator().next();
				translated.add(Equality.createAttributeValue(first, constantValue));
			}
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeID first = blankNodeIDs.iterator().next();
				translated.add(first.valueExpression(constantValue));
			}
			if (!patterns.isEmpty()) {
				Pattern first = patterns.iterator().next();
				translated.add(first.valueExpression(constantValue));
			}
			if (!expressions.isEmpty()) {
				Expression first = expressions.iterator().next();
				translated.add(Equality.createExpressionValue(first, constantValue));
			}
		} else if (!attributes.isEmpty()) {
			AttributeExpr attribute = new AttributeExpr(attributes.iterator().next());
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeID first = blankNodeIDs.iterator().next();
				translated.add(Equality.create(attribute, first.toExpression()));
			}
			if (!patterns.isEmpty()) {
				Pattern first = patterns.iterator().next();
				translated.add(Equality.create(attribute, first.toExpression()));
			}
			if (!expressions.isEmpty()) {
				Expression first = expressions.iterator().next();
				translated.add(Equality.create(attribute, first));
			}
		} else if (!expressions.isEmpty()) {
			Expression expression = expressions.iterator().next();
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeID first = blankNodeIDs.iterator().next();
				translated.add(Equality.create(expression, first.toExpression()));
			}
			if (!patterns.isEmpty()) {
				Pattern first = patterns.iterator().next();
				translated.add(Equality.create(expression, first.toExpression()));
			}
		} else if (!patterns.isEmpty()) {
			Expression pattern = (patterns.iterator().next()).toExpression();
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeID first = blankNodeIDs.iterator().next();
				translated.add(Equality.create(pattern, first.toExpression()));
			}
		}
		return Conjunction.create(translated);
	}
}
