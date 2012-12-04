package org.d2rq.nodes;

import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.values.ValueMaker;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;


/**
 * A {@link NodeMaker} that produces nodes from an underlying
 * {@link ValueMaker} according to a {@link NodeType}.
 * 
 * TODO: isUnique() should probably not be stored here, but derived from unique key information in the underlying table(s). d2rq:containsDuplicates should be treated as asserting a unique key.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TypedNodeMaker implements NodeMaker {
	public final static NodeType URI = new URINodeType();
	public final static NodeType BLANK = new BlankNodeType();
	public final static NodeType PLAIN_LITERAL = new LiteralNodeType("", null);
	public final static NodeType XSD_DATE = new DateLiteralNodeType();
	public final static NodeType XSD_TIME = new TimeLiteralNodeType();
	public final static NodeType XSD_DATETIME = new DateTimeLiteralNodeType();
	public final static NodeType XSD_BOOLEAN = new BooleanLiteralNodeType();
	
	public static NodeType languageLiteral(String language) {
		return new LiteralNodeType(language, null);
	}
	public static NodeType typedLiteral(RDFDatatype datatype) {
		// TODO These subclasses can be abolished; just introduce an RDFDatatypeValidator instead
		if (datatype.equals(XSDDatatype.XSDdate)) {
			return XSD_DATE;
		}
		if (datatype.equals(XSDDatatype.XSDtime)) {
			return XSD_TIME;
		}
		if (datatype.equals(XSDDatatype.XSDdateTime)) {
			return XSD_DATETIME;
		}
		if (datatype.equals(XSDDatatype.XSDboolean)) {
			return XSD_BOOLEAN;
		}
		return new LiteralNodeType("", datatype);
	}
	
	private NodeType nodeType;
	private ValueMaker valueMaker;
	
	public TypedNodeMaker(NodeType nodeType, ValueMaker valueMaker) {
		this.nodeType = nodeType;
		this.valueMaker = valueMaker;
	}
	
	public NodeType getNodeType() {
		return nodeType;
	}
	
	public ValueMaker getValueMaker() {
		return valueMaker;
	}
	
	public Set<ProjectionSpec> projectionSpecs() {
		return this.valueMaker.projectionSpecs();
	}
	
	public void describeSelf(NodeSetFilter c) {
		this.nodeType.matchConstraint(c);
		this.valueMaker.describeSelf(c);
	}

	public Node makeNode(ResultRow tuple) {
		String value = this.valueMaker.makeValue(tuple);
		if (value == null) {
			return null;
		}
		return this.nodeType.makeNode(value);
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		// TODO: Consider the node type (e.g., RDF datatype) in ordering,
		//       rather than just deferring to the underlying value maker
		return valueMaker.orderSpecs(ascending);
	}

	public String toString() {
		return this.nodeType.toString() + "(" + this.valueMaker + ")";
	}

	public void accept(NodeMakerVisitor visitor) {
		visitor.visit(this);
	}

	public interface NodeType {
		String extractValue(Node node);
		Node makeNode(String value);
		void matchConstraint(NodeSetFilter c);
		boolean matches(Node node);
	}
	
	private static class URINodeType implements NodeType {
		public String extractValue(Node node) { return node.getURI(); }
		public Node makeNode(String value) { return Node.createURI(value); }
		public void matchConstraint(NodeSetFilter c) { c.limitToURIs(); }
		public boolean matches(Node node) { return node.isURI(); }
		public String toString() { return "URI"; }
	}
	
	private static class BlankNodeType implements NodeType {
		public String extractValue(Node node) { return node.getBlankNodeLabel(); }
		public Node makeNode(String value) { return Node.createAnon(new AnonId(value)); }
		public void matchConstraint(NodeSetFilter c) { c.limitToBlankNodes(); }
		public boolean matches(Node node) { return node.isBlank(); }
		public String toString() { return "Blank"; }
	}
	
	private static class LiteralNodeType implements NodeType {
		private String language;
		private RDFDatatype datatype;
		LiteralNodeType(String language, RDFDatatype datatype) {
			this.language = language;
			this.datatype = datatype;
		}
		public String extractValue(Node node) {
			return node.getLiteralLexicalForm();
		}
		public Node makeNode(String value) {
			return Node.createLiteral(value, this.language, this.datatype);
		}
		public void matchConstraint(NodeSetFilter c) {
	        c.limitToLiterals(this.language, this.datatype);
		}
		public boolean matches(Node node) { 
			return node.isLiteral()
					&& this.language.equals(node.getLiteralLanguage())
					&& ((this.datatype == null && node.getLiteralDatatype() == null)
							|| (this.datatype != null && this.datatype.equals(node.getLiteralDatatype())));
		}
		public String toString() {
			StringBuffer result = new StringBuffer("Literal");
			if (!"".equals(this.language)) {
				result.append("@" + this.language);
			}
			if (this.datatype != null) {
				result.append("^^");
				result.append(PrettyPrinter.toString(this.datatype));
			}
			return result.toString();
		}
	}
	
	private static class DateLiteralNodeType extends LiteralNodeType {
		DateLiteralNodeType() {
			super("", XSDDatatype.XSDdate);
		}
		public boolean matches(Node node) {
			return super.matches(node) && XSDDatatype.XSDdate.isValid(node.getLiteralLexicalForm());
		}
		public Node makeNode(String value) {
			if (!XSDDatatype.XSDdate.isValid(value)) return null;
			return Node.createLiteral(value, null, XSDDatatype.XSDdate);
		}
	}
	
	private static class TimeLiteralNodeType extends LiteralNodeType {
		TimeLiteralNodeType() {
			super("", XSDDatatype.XSDtime);
		}
		public boolean matches(Node node) {
			return super.matches(node) && XSDDatatype.XSDtime.isValid(node.getLiteralLexicalForm());
		}
		public Node makeNode(String value) {
			if (!XSDDatatype.XSDtime.isValid(value)) return null;
			return Node.createLiteral(value, null, XSDDatatype.XSDtime);
		}
	}
	
	private static class DateTimeLiteralNodeType extends LiteralNodeType {
		DateTimeLiteralNodeType() {
			super("", XSDDatatype.XSDdateTime);
		}
		public boolean matches(Node node) {
			return super.matches(node) && XSDDatatype.XSDdateTime.isValid(node.getLiteralLexicalForm());
		}
		public Node makeNode(String value) {
			if (!XSDDatatype.XSDdateTime.isValid(value)) return null;
			return Node.createLiteral(value, null, XSDDatatype.XSDdateTime);
		}
	}
	
	private static class BooleanLiteralNodeType extends LiteralNodeType {
		private final static Node TRUE = Node.createLiteral("true", null, XSDDatatype.XSDboolean);
		private final static Node FALSE = Node.createLiteral("false", null, XSDDatatype.XSDboolean);
		BooleanLiteralNodeType() {
			super("", XSDDatatype.XSDboolean);
		}
		public boolean matches(Node node) {
			return super.matches(node) && XSDDatatype.XSDboolean.isValid(node.getLiteralLexicalForm());
		}
		public Node makeNode(String value) {
			if ("0".equals(value) || "false".equals(value)) return FALSE;
			if ("1".equals(value) || "true".equals(value)) return TRUE;
			return null;
		}
	}
}
