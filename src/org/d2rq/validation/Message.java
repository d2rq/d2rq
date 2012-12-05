package org.d2rq.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.d2rq.pp.PrettyPrinter;
import org.d2rq.r2rml.MappingTerm;
import org.d2rq.r2rml.RDFComparator;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class Message {

	public interface Renderer {
		void render(Message message);
	}
	
	public enum Level { Error, Warning, Info }
	
	public enum Problem {
		// Information about D2RQ implementation limitations
		NO_SQL_CONNECTION(Level.Info, "Not validating schema",
				"No database connection was specified. Table/column names and SQL queries in the mapping will not be validated."),
		INVERSE_EXPRESSION_NOT_VALIDATED(Level.Info, "rr:inverseExpression not validated",
				"Validation of the semantics of rr:inverseExpression is not implemented."),

		// Pre-R2RML checks
		IO_ERROR(Level.Error, "I/O error when reading mapping document"),
		SYNTAX_ERROR(Level.Error, "Turtle syntax error",
				"The mapping document is not a valid Turtle file: {details}"),

		// Graph checks
		NO_TRIPLES(Level.Error, "Empty document", 
				"The document was successfully parsed, but contains no triples."),
		NO_R2RML_TRIPLES(Level.Error, "Not an R2RML document",
				"The document was successfully parsed, but contains no properties or classes in the R2RML namespace."),
		UNKNOWN_CLASS_IN_R2RML_NAMESPACE(Level.Warning, "Undefined class {resource}",
				"The class {resource} is used in the document, but is not defined in R2RML."),
		UNKNOWN_PROPERTY_IN_R2RML_NAMESPACE(Level.Warning, "Undefined property {resource}",
				"The property {resource} is used in the document, but is not defined in R2RML."),
		UNKNOWN_RESOURCE_IN_R2RML_NAMESPACE(Level.Warning, "Undefined resource {resource}",
				"The resource {resource} is used in the document, but is not defined in R2RML."),
		SPURIOUS_TYPE(Level.Warning, "Spurious type statement",
				"The resource {resource} is typed as an {object}, but it lacks the required properties."),
		SPURIOUS_TRIPLE(Level.Warning, "Spurious property",
				"The resource {resource} has a property {property} that has no effect on a resource of its type."),
		
		// General domain stuff
		CONFLICTING_PROPERTIES(Level.Error, "Conflicting properties",
				"The resource {resource} can have only one of the properties {properties}."),
		CONFLICTING_TYPES(Level.Error, "Ambiguous type",
				"The property {property} of {resource} can only have one of the types {objects}."),
		REQUIRED_VALUE_MISSING(Level.Error, "Requiring value for property {properties|or}",
				"The resource {resource} requires a value for the property {properties|or}."),
		DUPLICATE_VALUE(Level.Error, "Duplicate value for {property}",
				"The resource {resource} has multiple values for {property} ({objects}); only one is allowed."),
		VALUE_NOT_OF_EXPECTED_TYPE(Level.Error, "Value not of expected type",
				"The  {property} of {resource} must be an {objects|or}, but does not have the necessary properties."),
		VALUE_MUST_BE_STRING_LITERAL(Level.Error, "{property} must be string literal",
				"The property {property} of {resource} ({object}) must be a string literal instead of a {object|nodetype}."),
		VALUE_MUST_BE_IRI(Level.Error, "{property} must be IRI",
				"The property {property} of {resource} ({object}) must be an IRI instead of a {object|nodetype}."),
		VALUE_MUST_BE_IRI_OR_LITERAL(Level.Error, "{property} must be IRI or literal",
				"The property {property} of {resource} ({object}) must be an IRI or a literal instead of a blank node."),
		VALUE_MUST_BE_IRI_OR_BLANK_NODE(Level.Error, "{property} must be IRI or blank node",
				"The property {property} or {resource} ({object}) must be an IRI or a blank node instead of a literal."),

		// rr:RefObjectMap
		JOIN_REQUIRED(Level.Error, "Join required",
				"The referencing object map {resource} requires an rr:joinCondition because it links two different logical tables."),

		// rr:class, rr:sqlVersion, rr:datatype
		INVALID_IRI(Level.Error, "Malformed IRI",
				"Malformed IRI {object} in property {property} of {resource}: {details}."),
		IRI_NOT_ABSOLUTE(Level.Error, "Not an absolute IRI",
				"The property {property} of {resource} ({object}) must be an absolute IRI."),
		IRI_WARNING(Level.Warning, "IRI syntax warning",
				"The property {property} of {resource} ({object}) is not a recommended IRI: {details}."),

		// rr:TermMap
		COLUMN_NOT_IN_LOGICAL_TABLE(Level.Error, "Column {string} not in logical table",
				"The column {string}, specified in the {property} of {resource}, does not exist in the logical table."),
		
		// rr:tableName
		MALFORMED_TABLE_OR_VIEW_NAME(Level.Error, "Malformed table name {string}",
				"Malformed table or view name {string} in property {property} of {resource}: {details}."),
		NO_SUCH_TABLE_OR_VIEW(Level.Error, "Table or view {string} does not exist",
				"The property {property} of {resource} ({string}) is not the name of a table or view in the database."),
		
		// rr:sqlQuery
		INVALID_SQL_QUERY(Level.Error, "Invalid SQL query",
				"Invalid SQL query in property {property} of {resource}: {details}. Query was: {string}"),
		DUPLICATE_COLUMN_NAME_IN_SQL_QUERY(Level.Error, "Duplicate column name in SQL query",
				"The {property} of {resource} contains a duplicate column name: {object}."),

		// rr:language
		INVALID_LANGUAGE_TAG(Level.Error, "Malformed language tag {object}",
				"Malformed language tag {object} in property {property} of {resource}: {details}."),

		// rr:columnName, rr:parent, rr:child
		INVALID_COLUMN_NAME(Level.Error, "Malformed column name {string}",
				"Malformed column name {string} in property {property} of {resource}: {details}."),

		// rr:template, rr:inverseExpression
		INVALID_STRING_TEMPLATE(Level.Error, "Malformed template {object}",
				"Malformed template {object} in property {property} of {resource}: {details}."),
		INVALID_IRI_TEMPLATE(Level.Error, "Malformed IRI template {object}",
				"Malformed IRI template {object} in property {property} of {resource}: {details}."),
		STRING_TEMPLATE_WITHOUT_COLUMN_NAME(Level.Warning, "String template without column name",
				"The template {object} in property {property} of {resource} does not contain any column names."),
		EMPTY_SEPARATOR_IN_STRING_TEMPLATE(Level.Warning, "Empty separator in template",
				"The template {object} in property {property} of {resource} has column references without separating characters between them."),
		POSSIBLE_UNSAFE_SEPARATOR_IN_IRI_TEMPLATE(Level.Warning, "Possible unsafe separator in IRI template",
				"Column references in the {property} of {resource} ({object}) are separated by a possibly unsafe delimiter. It is recommended that IRI sub-delim characters are used to delimit column references in IRI templates."),
		INVALID_INVERSE_EXPRESSION(Level.Error, "Invalid inverse expression",
				"Invalid inverse expression {object} in property {property} of {resource}: {details}."),

		// rr:termType
		INVALID_TERM_TYPE(Level.Error, "Invalid term type {object}",
				"The property rr:termType of {resource} is {object}, but only the values rr:IRI, rr:Literal or rr:BlankNode are allowed."),
		
		// rr:datatype, rr:language
		ONLY_ALLOWED_IF_TERM_TYPE_LITERAL(Level.Error, "{property} not allowed for this term type",
				"The property {property} is only allowed on term maps that generate literals, but the rr:termType of {resource} is not rr:Literal."),

		// rr:BlankNode, rr:Literal
		ONLY_ALLOWED_ON_SUBJECT_OR_OBJECT_MAP(Level.Error, "Term type rr:BlankNode not allowed here",
				"{resource} has an rr:termType of rr:BlankNode, but this is only allowed on subject maps or object maps."),
		ONLY_ALLOWED_ON_OBJECT_MAP(Level.Error, "Term type rr:Literal not alowed here",
				"{resource} has an rr:termType of rr:Literal, but this is only allowed on object maps."),

		// rr:graph, rr:graphMap
		ONLY_ACTIVE_ON_SUBJECT_MAP(Level.Warning, "Graph map is ignored",
				"{resource} has a spurious {property} property, as graph maps only have an effect when specified on subject maps or predicate-object maps."),

		// Mapping
		NO_TRIPLES_MAP(Level.Error, "No triples maps",
				"No triples maps were found in the document. A triples map requires at least an rr:logicalTable."),
		UNREFERENCED_MAPPING_COMPONENT(Level.Warning, "Unreferenced {string}",
				"The resource {resource} is defined as a {string} in the mapping, but never used.");
		
		private final Level level;
		private final String title;
		private final String template;
		Problem(Level level) { this(level, null, null); }
		Problem(Level level, String title) { this(level, title, null); }
		Problem(Level level, String title, String template) {
			this.level = level;
			this.title = title;
			this.template = template;
		}
		public Level getLevel() { return level; }
		public String getTitle() { return title == null ? name() : title; }
		public String getTemplate() { return template; }
	}
	
	private final Problem problem;
	private final Resource subject;
	private final List<Property> predicates;
	private final List<RDFNode> objects;
	private final String detailCode;
	private final String details;

	public Message(Problem problem) {
		this(problem, null, null, null);
	}
	
	public Message(Problem problem, String details) {
		this(problem, null, null, details, null, null);
	}
	
	/**
	 * @param problem
	 * @param subject May be null
	 * @param predicates May be null
	 * @param objects May be null
	 */
	public Message(Problem problem, Resource subject,
			Property[] predicates, RDFNode[] objects) {
		this.problem = problem;
		this.subject = subject;
		this.predicates = 
			predicates == null 
					? Collections.<Property>emptyList() 
					: Arrays.asList(predicates);
		Collections.sort(this.predicates, RDFComparator.getRDFNodeComparator());
		this.objects = 
			objects == null 
					? Collections.<RDFNode>emptyList() 
					: Arrays.asList(objects);
		Collections.sort(this.objects, RDFComparator.getRDFNodeComparator());
		this.detailCode = null;
		this.details = null;
	}
	
	/**
	 * @param problem
	 * @param term
	 * @param detailCode Optional error code; indicates a subclass of problems
	 * @param details Optional string containing error details
	 * @param contextResource May be null
	 * @param contextProperty May be null
	 */
	public Message(Problem problem, MappingTerm term, 
			String detailCode, String details, 
			Resource contextResource, Property contextProperty) {
		this.problem = problem;
		this.subject = contextResource; 
		this.predicates = 
			contextProperty == null 
					? Collections.<Property>emptyList() 
					: Collections.singletonList(contextProperty);
		this.objects = 
			term == null
					? Collections.<RDFNode>emptyList()
					: Collections.<RDFNode>singletonList(ResourceFactory.createPlainLiteral(term.toString()));
		this.detailCode = detailCode;
		this.details = details;
	}
	
	public Level getLevel() {
		return problem.getLevel();
	}
	
	public Problem getProblem() {
		return problem;
	}
	
	public String getProblemTitle() {
		return problem.getTitle();
	}
	
	public Resource getSubject() {
		return subject;
	}
	
	public Property getPredicate() {
		if (predicates.isEmpty()) return null;
		return predicates.get(0);
	}
	
	public List<Property> getPredicates() {
		return predicates;
	}
	
	public RDFNode getObject() {
		if (objects.isEmpty()) return null;
		return objects.get(0);
	}
	
	public List<RDFNode> getObjects() {
		return objects;
	}
	
	public String getValue() {
		if (objects.isEmpty()) return null;
		return objects.get(0).asLiteral().getLexicalForm();
	}
	
	public String getDetailCode() {
		return detailCode;
	}
	
	public String getDetails() {
		return details;
	}
	
	public String getTemplate() {
		return problem.getTemplate();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(problem.name());
		if (detailCode != null) {
			s.append('/');
			s.append(detailCode);
		}
		s.append('(');
		s.append(problem.getLevel());
		s.append(')');
		if (subject != null) {
			s.append("; resource: ");
			s.append(PrettyPrinter.toString(subject));
		}
		if (!predicates.isEmpty()) {
			s.append("; predicate");
			if (predicates.size() > 1) {
				s.append('s');
			}
			s.append(": ");
			for (Property property: predicates) {
				s.append(PrettyPrinter.toString(property));
				s.append(", ");
			}
			s.delete(s.length() - 2, s.length());
		}
		if (!objects.isEmpty()) {
			s.append("; object");
			if (objects.size() > 1) {
				s.append('s');
			}
			s.append(": ");
			for (RDFNode object: objects) {
				s.append(PrettyPrinter.toString(object));
				s.append(", ");
			}
			s.delete(s.length() - 2, s.length());
		}
		if (details != null) {
			s.append(": ");
			s.append(details);
		}
		return s.toString();
	}
}
