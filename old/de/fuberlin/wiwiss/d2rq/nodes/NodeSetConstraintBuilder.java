package de.fuberlin.wiwiss.d2rq.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.values.BlankNodeIDValueMaker;
import org.d2rq.values.TemplateValueMaker;
import org.d2rq.values.Translator;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;


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
	private Collection<ColumnName> columns = new HashSet<ColumnName>();
	private Collection<TemplateValueMaker> patterns = new HashSet<TemplateValueMaker>();
	private Collection<Expression> expressions = new HashSet<Expression>();
	private Collection<BlankNodeIDValueMaker> blankNodeIDs = new HashSet<BlankNodeIDValueMaker>();
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
		for (TemplateValueMaker pattern: patterns) {
			if (!pattern.matches(constant)) {
				limitToEmptySet();
				return;
			}
		}
		for (BlankNodeIDValueMaker id: blankNodeIDs) {
			if (!id.matches(constant)) {
				limitToEmptySet();
				return;
			}
		}
	}

	public void limitValuesToColumn(ColumnName column) {
		if (isEmpty) return;
		columns.add(column);
	}

	public void limitValuesToBlankNodeID(BlankNodeIDValueMaker id) {
		if (isEmpty) return;
		if (!blankNodeIDs.isEmpty()) {
			BlankNodeIDValueMaker first = (BlankNodeIDValueMaker) blankNodeIDs.iterator().next();
			if (!first.getID().equals(id.getID())) {
				limitToEmptySet();
			}
		}
		blankNodeIDs.add(id);
	}

	public void limitValuesToPattern(TemplateValueMaker pattern) {
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
	
	private List<Expression> matchPatterns(TemplateValueMaker p1, TemplateValueMaker p2) {
		List<Expression> results = new ArrayList<Expression>(p1.columns().length);
		if (p1.isEquivalentTo(p2)) {
			for (int i = 0; i < p1.columns().length; i++) {
				results.add(Equality.createColumnEquality(p1.columns()[i], p2.columns()[i]));
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
	
	private List<Expression> matchBlankNodeIDs(BlankNodeIDValueMaker id1, BlankNodeIDValueMaker id2) {
		List<Expression> results = new ArrayList<Expression>(id1.getColumns().size());
		for (int i = 0; i < id1.getColumns().size(); i++) {
			ColumnName col1 = id1.getColumns().get(i);
			ColumnName col2 = id2.getColumns().get(i);
			results.add(Equality.createColumnEquality(col1, col2));
		}
		return results;
	}
	
	public Expression constraint() {
		if (isEmpty()) {
			return Expression.FALSE;
		}
		List<Expression> translated = new ArrayList<Expression>();
		if (columns.size() >= 2) {
			Iterator<ColumnName> it = columns.iterator();
			ColumnName first = it.next();
			while (it.hasNext()) {
				translated.add(Equality.createColumnEquality(first, it.next()));
			}
		}
		if (patterns.size() >= 2) {
			Iterator<TemplateValueMaker> it = patterns.iterator();
			TemplateValueMaker first = it.next();
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
			Iterator<BlankNodeIDValueMaker> it = blankNodeIDs.iterator();
			BlankNodeIDValueMaker first = it.next();
			while (it.hasNext()) {
				translated.addAll(matchBlankNodeIDs(first, it.next()));
			}
		}
		if (constantValue != null) {
			if (!columns.isEmpty()) {
				ColumnName first = columns.iterator().next();
				translated.add(Equality.createColumnValue(first, constantValue));
			}
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeIDValueMaker first = blankNodeIDs.iterator().next();
				translated.add(first.valueExpression(constantValue));
			}
			if (!patterns.isEmpty()) {
				TemplateValueMaker first = patterns.iterator().next();
				translated.add(first.valueExpression(constantValue));
			}
			if (!expressions.isEmpty()) {
				Expression first = expressions.iterator().next();
				translated.add(Equality.createExpressionValue(first, constantValue));
			}
		} else if (!columns.isEmpty()) {
			ColumnExpr attribute = new ColumnExpr(columns.iterator().next());
			if (!blankNodeIDs.isEmpty()) {
				BlankNodeIDValueMaker first = blankNodeIDs.iterator().next();
				translated.add(Equality.create(attribute, first.toExpression()));
			}
			if (!patterns.isEmpty()) {
				TemplateValueMaker first = patterns.iterator().next();
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
				BlankNodeIDValueMaker first = blankNodeIDs.iterator().next();
				translated.add(Equality.create(expression, first.toExpression()));
			}
			if (!patterns.isEmpty()) {
				TemplateValueMaker first = patterns.iterator().next();
				translated.add(Equality.create(expression, first.toExpression()));
				checkUsesColumnFunctions(first);
			}
		} else if (!patterns.isEmpty() && !blankNodeIDs.isEmpty()) {
			TemplateValueMaker firstPattern = patterns.iterator().next();
			BlankNodeIDValueMaker firstBNodeID = blankNodeIDs.iterator().next();
			translated.add(Equality.create(firstPattern.toExpression(), firstBNodeID.toExpression()));
			checkUsesColumnFunctions(firstPattern);
		}
		// FIXME: Actually handle this properly, see https://github.com/d2rq/d2rq/issues/22
		if (translators.size() > 1) {
			log.warn("Join involving multiple translators (d2rq:translateWith) is not supported");
		}
		return Conjunction.create(translated);
	}
	
	private void checkUsesColumnFunctions(TemplateValueMaker pattern) {
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
