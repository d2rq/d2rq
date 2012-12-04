package org.d2rq.r2rml;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;


public abstract class TermMap extends MappingComponent {

	public enum TermType { 
		IRI(RR.IRI), 
		BLANK_NODE(RR.BlankNode), 
		LITERAL(RR.Literal);
		
		private final Resource resource;
		TermType(Resource resource) {
			this.resource = resource;
		}
		public Resource asResource() {
			return resource;
		}
		public static TermType getFor(Resource resource) {
			for (TermType type: TermType.values()) {
				if (type.asResource().equals(resource)) return type;
			}
			return null;
		}
	}

	public enum Position {
		SUBJECT_MAP(RR.SubjectMap, new TermType[]{TermType.IRI, TermType.BLANK_NODE}),
		PREDICATE_MAP(RR.PredicateMap, new TermType[]{TermType.IRI}),
		OBJECT_MAP(RR.ObjectMap, new TermType[]{TermType.IRI, TermType.BLANK_NODE, TermType.LITERAL}),
		GRAPH_MAP(RR.GraphMap, new TermType[]{TermType.IRI});
		
		private final Resource r2rmlTerm;
		private final TermType[] allowedTermTypes;
		Position(Resource r2rmlTerm, TermType[] allowedTermTypes) {
			this.r2rmlTerm = r2rmlTerm;
			this.allowedTermTypes = allowedTermTypes;
		}
		public Resource getR2RMLTerm() {
			return r2rmlTerm;
		}
		public boolean isAllowedTermType(TermType termType) {
			for (TermType allowed: allowedTermTypes) {
				if (termType == allowed) return true;
			}
			return false;
		}
		public static Position getFor(Resource r2rmlTerm) {
			for (Position p: Position.values()) {
				if (p.getR2RMLTerm().equals(r2rmlTerm)) return p;
			}
			return null;
		}
	}
	
	/**
	 * This is usable either as a regular or as a simple mapping component.
	 */
	public static class ConstantValuedTermMap extends TermMap {
		private RDFNode constant = null;
		public ComponentType getType() {
			return ComponentType.CONSTANT_VALUED_TERM_MAP;
		}
		public void setConstant(RDFNode constant) {
			this.constant = constant;
		}
		public RDFNode getConstant() {
			return constant;
		}
		@Override
		public TermType getTermType(Position position) {
			if (constant.isLiteral()) return TermType.LITERAL;
			if (constant.isURIResource()) return TermType.IRI;
			return TermType.BLANK_NODE;
		}
		@Override
		public void acceptAs(MappingVisitor visitor, Position position) {
			super.acceptAs(visitor, position);
			visitor.visitComponent(this, position);
			visitor.visitSimpleProperty(RR.constant, constant);
		}
	}

	public static abstract class ColumnOrTemplateValuedTermMap extends TermMap {
		private TermType termType = null;
		private LanguageTag languageTag = null;
		private ConstantIRI datatype = null;
		private StringTemplate inverseExpression = null;
		public void setSpecifiedTermType(TermType termType) {
			this.termType = termType;
		}
		/**
		 * The term type as specified by rr:termType, or null if the property is not present
		 */
		public TermType getSpecifiedTermType() {
			return termType;
		}		
		public void setLanguageTag(LanguageTag languageTag) {
			this.languageTag = languageTag;
		}
		public LanguageTag getLanguageTag() {
			return languageTag;
		}
		public void setDatatype(ConstantIRI datatype) {
			this.datatype = datatype;
		}
		public ConstantIRI getDatatype() {
			return datatype;
		}
		public void setInverseExpression(StringTemplate inverseExpression) {
			this.inverseExpression = inverseExpression;
		}
		public StringTemplate getInverseExpression() {
			return inverseExpression;
		}
		/**
		 * The default term type if not overridden by rr:termType or
		 * presence of rr:datatype or rr:language 
		 */
		public abstract TermType getDefaultObjectMapTermType();
		public TermType getTermType(Position position) {
			if (getSpecifiedTermType() != null) return getSpecifiedTermType();
			if (position != Position.OBJECT_MAP) {
				return TermType.IRI;
			}
			if (getLanguageTag() != null || getDatatype() != null) {
				return TermType.LITERAL;
			}
			return getDefaultObjectMapTermType();
		}
		@Override
		public void acceptAs(MappingVisitor visitor, Position position) {
			super.acceptAs(visitor, position);
			visitor.visitComponent(this, position);
			visitor.visitTermProperty(RR.datatype, datatype);
			visitor.visitTermProperty(RR.language, languageTag);
			visitor.visitTermProperty(RR.inverseExpression, inverseExpression);
			visitor.visitSimpleProperty(RR.termType, termType);
		}
	}
	
	public static class ColumnValuedTermMap extends ColumnOrTemplateValuedTermMap {
		private ColumnNameR2RML column = null;
		public ComponentType getType() {
			return ComponentType.COLUMN_VALUED_TERM_MAP;
		}
		public void setColumnName(ColumnNameR2RML column) {
			this.column = column;
		}
		public ColumnNameR2RML getColumnName() {
			return column;
		}
		@Override
		public TermType getDefaultObjectMapTermType() {
			return TermType.LITERAL;
		}
		@Override
		public void acceptAs(MappingVisitor visitor, Position position) {
			super.acceptAs(visitor, position);
			visitor.visitComponent(this, position);
			visitor.visitTermProperty(RR.column, column);
		}
	}
	
	public static class TemplateValuedTermMap extends ColumnOrTemplateValuedTermMap {
		private StringTemplate template = null;
		public ComponentType getType() {
			return ComponentType.TEMPLATE_VALUED_TERM_MAP;
		}
		public void setTemplate(StringTemplate template) {
			this.template = template;
		}
		public StringTemplate getTemplate() {
			return template;
		}
		@Override
		public TermType getDefaultObjectMapTermType() {
			return TermType.IRI;
		}
		@Override
		public void acceptAs(MappingVisitor visitor, Position position) {
			super.acceptAs(visitor, position);
			visitor.visitComponent(this, position);
			visitor.visitTermProperty(RR.template, template);
		}
	}
	
	private final Set<ConstantIRI> classes = new HashSet<ConstantIRI>();
	private final Set<Resource> graphMaps = new HashSet<Resource>();
	private final Set<ConstantShortcut> graphs = new HashSet<ConstantShortcut>();
	
	public Set<ConstantIRI> getClasses() {
		return classes;
	}
	
	public Set<Resource> getGraphMaps() {
		return graphMaps;
	}
	
	public Set<ConstantShortcut> getGraphs() {
		return graphs;
	}
	
	public abstract TermType getTermType(Position position);
	
	@Override
	public final void accept(MappingVisitor visitor) {
		acceptAs(visitor, null);
	}
	
	public void acceptAs(MappingVisitor visitor, Position position) {
		visitor.visitComponent(this, position);
		if (position == Position.SUBJECT_MAP) {
			for (ConstantIRI iri: classes) {
				visitor.visitTermProperty(RR.class_, iri);
			}
			for (Resource graphMap: graphMaps) {
				visitor.visitComponentProperty(RR.graphMap, graphMap, ComponentType.GRAPH_MAP);
			}
			for (ConstantShortcut graph: graphs) {
				visitor.visitTermProperty(RR.graph, graph);
			}
		}
	}
}
