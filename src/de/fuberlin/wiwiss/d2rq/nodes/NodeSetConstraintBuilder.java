package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.Translator;

/**
 * Builds up an {@link Expression} that expresses restrictions
 * on a set of nodes.
 * 
 * Used for expressing the constraints on a variable that is
 * shared between node relations.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeSetConstraintBuilder implements NodeSetFilter {
	private final static Log log = LogFactory.getLog(NodeSetConstraintBuilder.class);
	
	private final static int NODE_TYPE_UNKNOWN = 0;
	private final static int NODE_TYPE_URI = 1;
	private final static int NODE_TYPE_LITERAL = 2;
	private final static int NODE_TYPE_BLANK = 3;
	
	private boolean isEmpty = false;
	private boolean unsupported = false;
	private int type = NODE_TYPE_UNKNOWN;
	private String constantValue = null;
	private String constantLanguage = null;
	private RDFDatatype constantDatatype = null;
	private Node fixedNode = null;
	private Collection<Attribute> attributes = new HashSet<Attribute>();
	private Collection<Pattern> patterns = new HashSet<Pattern>();
	private Collection<Expression> expressions = new HashSet<Expression>();
	private Collection<BlankNodeID> blankNodeIDs = new HashSet<BlankNodeID>();
	private Set<Translator> translators = new HashSet<Translator>();
	private String valueStart = "";
	private String valueEnd = "";
	
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
		if (isEmpty) return;
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
		if (isEmpty) return;
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
		if (isEmpty) return;
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
		if (isEmpty) return;
		if (constantValue == null) {
			constantValue = constant;
		} else if (!constantValue.equals(constant)) {
			limitToEmptySet();
			return;
		}
		if (valueStart != null && !constant.startsWith(valueStart)) {
			limitToEmptySet();
			return;
		}
		valueStart = constant;
		if (valueEnd != null && !constant.endsWith(valueEnd)) {
			limitToEmptySet();
			return;
		}
		valueEnd = constant;
		for (Pattern pattern: patterns) {
			if (!pattern.matches(constant)) {
				limitToEmptySet();
				return;
			}
		}
		for (BlankNodeID id: blankNodeIDs) {
			if (!id.matches(constant)) {
				limitToEmptySet();
				return;
			}
		}
	}

	public void limitValuesToAttribute(Attribute attribute) {
		if (isEmpty) return;
		attributes.add(attribute);
	}

	public void limitValuesToBlankNodeID(BlankNodeID id) {
		if (isEmpty) return;
		if (!blankNodeIDs.isEmpty()) {
			BlankNodeID first = (BlankNodeID) blankNodeIDs.iterator().next();
			if (!first.classMapID().equals(id.classMapID())) {
				limitToEmptySet();
			}
		}
		blankNodeIDs.add(id);
	}

	public void limitValuesToPattern(Pattern pattern) {
		if (isEmpty) return;
		patterns.add(pattern);
		if (pattern.firstLiteralPart().startsWith(valueStart)) {
			valueStart = pattern.firstLiteralPart();
		} else if (!valueStart.startsWith(pattern.firstLiteralPart())) {
			limitToEmptySet();
		}
		if (pattern.lastLiteralPart().endsWith(valueEnd)) {
			valueEnd = pattern.lastLiteralPart();
		} else if (!valueEnd.endsWith(pattern.lastLiteralPart())) {
			limitToEmptySet();
		}
		if (constantValue != null) {
			if (!pattern.matches(constantValue)) {
				limitToEmptySet();
			}
		}
	}

	public void limitValuesToExpression(Expression expression) {
		if (isEmpty) return;
		expressions.add(expression);
	}

	public void setUsesTranslator(Translator translator) {
		translators.add(translator);
		if (translators.size() > 1) {
			unsupported = true;
		}
	}

	public boolean isEmpty() {
		return isEmpty;
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
			// FIXME: Actually support it
			if (p1.usesColumnFunctions() || p2.usesColumnFunctions()) {
				log.warn("Joining multiple d2rq:[uri]Patterns with different @@|encoding@@ is not supported");
				unsupported = true;
			}
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
				checkUsesColumnFunctions(first);
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
				checkUsesColumnFunctions(first);
			}
		} else if (!patterns.isEmpty() && !blankNodeIDs.isEmpty()) {
			Pattern firstPattern = patterns.iterator().next();
			BlankNodeID firstBNodeID = blankNodeIDs.iterator().next();
			translated.add(Equality.create(firstPattern.toExpression(), firstBNodeID.toExpression()));
			checkUsesColumnFunctions(firstPattern);
		}
		// FIXME: Actually handle this properly, see https://github.com/d2rq/d2rq/issues/22
		if (translators.size() > 1) {
			log.warn("Join involving multiple translators (d2rq:translateWith) is not supported");
		}
		return Conjunction.create(translated);
	}
	
	private void checkUsesColumnFunctions(Pattern pattern) {
		if (!pattern.usesColumnFunctions()) return;
		// FIXME: Actually handle this properly, see https://github.com/d2rq/d2rq/issues/22
		unsupported = true;
		log.warn("Joining a d2rq:[uri]Pattern with any @@|encoding@@ to a " +
		"d2rq:[uri]Column or d2rq:[uri]Expression etc. is not supported");
	}
	
	public boolean isUnsupported() {
		constraint();	// unsupported is only set correctly after this call
		return unsupported;
	}
}
