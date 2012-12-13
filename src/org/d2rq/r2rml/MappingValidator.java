package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Identifier.IdentifierParseException;
import org.d2rq.db.schema.Identifier.ViolationType;
import org.d2rq.db.schema.TableName;
import org.d2rq.mapgen.IRIEncoder;
import org.d2rq.r2rml.LogicalTable.BaseTableOrView;
import org.d2rq.r2rml.LogicalTable.R2RMLView;
import org.d2rq.r2rml.MappingComponent.ComponentType;
import org.d2rq.r2rml.TermMap.ColumnOrTemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.ColumnValuedTermMap;
import org.d2rq.r2rml.TermMap.ConstantValuedTermMap;
import org.d2rq.r2rml.TermMap.Position;
import org.d2rq.r2rml.TermMap.TemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.TermType;
import org.d2rq.validation.Message.Problem;
import org.d2rq.validation.Report;
import org.d2rq.vocab.RR;

import com.hp.hpl.jena.iri.Violation;
import com.hp.hpl.jena.rdf.arp.lang.LanguageTagSyntaxException;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class MappingValidator extends MappingVisitor.TreeWalkerImplementation {
	private final Mapping mapping;
	private final SQLConnection sqlConnection;
	private final Stack<Resource> resourceStack = new Stack<Resource>();
	private final Stack<Property> propertyStack = new Stack<Property>();
	private Report report = new Report();
	private boolean throwExceptionOnError = false;
	private LogicalTable contextLogicalTable = null;
	private LogicalTable contextParentLogicalTable = null;
	
	public MappingValidator(Mapping mapping) {
		this(mapping, null);
	}

	public MappingValidator(Mapping mapping, SQLConnection sqlConnection) {
		super(mapping);
		this.mapping = mapping;
		this.sqlConnection = sqlConnection;
	}

	public void setReport(Report report) {
		this.report = report;
		report.setThrowExceptionOnError(throwExceptionOnError);
	}
	
	public void setThrowExceptionOnError(boolean flag) {
		throwExceptionOnError = flag;
		report.setThrowExceptionOnError(flag);
	}
	
	public Report getReport() {
		return report;
	}
	
	public void run() {
		mapping.accept(this);
	}
	
	@Override
	public void visitComponentProperty(Property property, Resource resource, 
			ComponentType... types) {
		validateComponentType(property, resource, types);
		propertyStack.push(property);
		resourceStack.push(resource);
		super.visitComponentProperty(property, resource, types);
		resourceStack.pop();
		propertyStack.pop();
	}

	@Override
	public void visitTermProperty(Property property, MappingTerm term) {
		propertyStack.push(property);
		super.visitTermProperty(property, term);
		propertyStack.pop();
	}

	@Override
	public void visitComponent(Mapping mapping) {
		if (mapping.triplesMaps().isEmpty()) {
			report.report(Problem.NO_TRIPLES_MAP);
		}
		Map<Resource,MappingComponent> unreferenced = 
			mapping.getUnreferencedMappingComponents();
		for (Resource r: unreferenced.keySet()) {
			report.report(Problem.UNREFERENCED_MAPPING_COMPONENT, r, null,
					ResourceFactory.createPlainLiteral(unreferenced.get(r).getType().getLabel()));
		}
		if (!report.hasError() && sqlConnection == null) {
			report.report(Problem.NO_SQL_CONNECTION);
		}
	}

	@Override
	public void visitComponent(TriplesMap triplesMap) {
		require(triplesMap, RR.logicalTable);
		if (triplesMap.getSubject() == null && triplesMap.getSubjectMap() == null) {
			report.report(Problem.REQUIRED_VALUE_MISSING, 
					getContextResource(),
					new Property[]{RR.subject, RR.subjectMap});
		}
		if (triplesMap.getSubject() != null && triplesMap.getSubjectMap() != null) {
			report.report(Problem.CONFLICTING_PROPERTIES, 
					getContextResource(),
					new Property[]{RR.subject, RR.subjectMap});
		}
		contextLogicalTable = getLogicalTable(triplesMap);
	}

	private LogicalTable getLogicalTable(TriplesMap triplesMap) {
		if (triplesMap == null || triplesMap.getLogicalTable() == null) return null;
		return mapping.logicalTables().get(triplesMap.getLogicalTable());
	}

	@Override
	public void visitComponent(LogicalTable logicalTable) {
		// Do nothing
	}

	@Override
	public void visitComponent(BaseTableOrView logicalTable) {
		require(logicalTable, RR.tableName);
	}

	@Override
	public void visitComponent(R2RMLView logicalTable) {
		require(logicalTable, RR.sqlQuery);
	}

	@Override
	public void visitComponent(ConstantValuedTermMap termMap, Position position) {
		require(termMap, RR.constant);
		if (termMap.getConstant() != null) {
			if (position == Position.OBJECT_MAP) {
				if (termMap.getTermType(position) == TermType.BLANK_NODE) {
					report.report(Problem.VALUE_MUST_BE_IRI_OR_LITERAL,
							getContextResource(),
							RR.constant, termMap.getConstant());
				}
			} else if (termMap.getTermType(position) != TermType.IRI) {
				report.report(Problem.VALUE_MUST_BE_IRI,  
						getContextResource(),
						RR.constant, termMap.getConstant());
			}
		}
	}

	@Override
	public void visitComponent(ColumnOrTemplateValuedTermMap termMap, Position position) {
		if (position != Position.OBJECT_MAP) {
			if (termMap.getLanguageTag() != null) {
				report.report(Problem.ONLY_ALLOWED_ON_OBJECT_MAP, 
						getContextResource(), RR.language);
			}
			if (termMap.getDatatype() != null) {
				report.report(Problem.ONLY_ALLOWED_ON_OBJECT_MAP, 
						getContextResource(), RR.datatype);
			}
		} else if (termMap.getTermType(position) != TermType.LITERAL) { 
			if (termMap.getLanguageTag() != null) {
				report.report(Problem.ONLY_ALLOWED_IF_TERM_TYPE_LITERAL, 
						getContextResource(), RR.language);
			}
			if (termMap.getDatatype() != null) {
				report.report(Problem.ONLY_ALLOWED_IF_TERM_TYPE_LITERAL, 
						getContextResource(), RR.datatype);
			}
		} else {
			if (termMap.getLanguageTag() != null && termMap.getDatatype() != null) {
				report.report(Problem.CONFLICTING_PROPERTIES,
						getContextResource(),
						new Property[]{RR.language, RR.datatype});
			}
		}
		if (termMap.getSpecifiedTermType() != null && !position.isAllowedTermType(termMap.getSpecifiedTermType())) {
			report.report(
					(termMap.getSpecifiedTermType() == TermType.BLANK_NODE)
							? Problem.ONLY_ALLOWED_ON_SUBJECT_OR_OBJECT_MAP 
							: Problem.ONLY_ALLOWED_ON_OBJECT_MAP,
					getContextResource(),
					RR.termType, termMap.getSpecifiedTermType().asResource());
		}

		// http://www.w3.org/TR/r2rml/#inverse
		if (sqlConnection != null && contextLogicalTable != null 
				&& contextLogicalTable.getColumns(sqlConnection) != null
				&& termMap.getInverseExpression() != null
				&& termMap.getInverseExpression().getColumnNames() != null) {
			for (String columnName: termMap.getInverseExpression().getColumnNames()) {
				try {
					Identifier column = sqlConnection.vendor().parseIdentifiers(columnName, 1, 1)[0];
					if (!contextLogicalTable.getColumns(sqlConnection).contains(column)) {
						report.report(Problem.COLUMN_NOT_IN_LOGICAL_TABLE, 
								getContextResource(), RR.inverseExpression,
								ResourceFactory.createPlainLiteral(column.toString()));
					}
				} catch (IdentifierParseException ex) {
					report.report(Problem.INVALID_COLUMN_NAME, 
							getContextResource(), RR.inverseExpression, 
							termMap.getInverseExpression(), 
							ex.getViolationType().name(), ex.getMessage());
				}
			}
		}
		if (termMap.getInverseExpression() != null) {
			// TODO: Fuller validation of inverse expressions
			report.report(Problem.INVERSE_EXPRESSION_NOT_VALIDATED, 
					getContextResource(), RR.inverseExpression,
					termMap.getInverseExpression());
		}
	}

	@Override
	public void visitComponent(ColumnValuedTermMap termMap, Position position) {
		require(termMap, RR.column);

		// "The referenced columns of all term maps of a triples map
		// (subject map, predicate maps, object maps, graph maps)
		// must be column names that exist in the term map's logical table."
		if (sqlConnection != null && contextLogicalTable != null 
				&& contextLogicalTable.getColumns(sqlConnection) != null
				&& termMap.getColumnName() != null 
				&& termMap.getColumnName().isValid(sqlConnection)) {
			Identifier column = termMap.getColumnName().asIdentifier(sqlConnection.vendor());
			if (column != null && !contextLogicalTable.getColumns(sqlConnection).contains(column)) {
				report.report(Problem.COLUMN_NOT_IN_LOGICAL_TABLE, 
						getContextResource(), RR.column, 
						ResourceFactory.createPlainLiteral(column.toString()));
			}
		}
	}

	@Override
	public void visitComponent(TemplateValuedTermMap termMap, Position position) {
		if (termMap.getTemplate() != null && termMap.getTemplate().isValid()
				&& termMap.getTermType(position) == TermType.IRI) {
			String[] literalParts = termMap.getTemplate().getLiteralParts();
			for (int i = 0; i < literalParts.length; i++) {
				for (int j = 0; j < literalParts[i].length(); j++) {
					char c = literalParts[i].charAt(j);
					if (!IRIEncoder.isAllowedInIRI(c)) {
						report.report(Problem.INVALID_IRI_TEMPLATE, 
								getContextResource(), RR.template,
								termMap.getTemplate(), 
								null, 
								"The character '" + c + "' is not allowed in IRIs");
					}
				}
				if (i > 0 && i < literalParts.length - 1 && 
						!literalParts[i].isEmpty() && !IRIEncoder.isSafeSeparator(literalParts[i])) {
					report.report(Problem.POSSIBLE_UNSAFE_SEPARATOR_IN_IRI_TEMPLATE, 
							getContextResource(), RR.template,
							termMap.getTemplate());
				}
			}
		}
		// "The referenced columns of all term maps of a triples map
		// (subject map, predicate maps, object maps, graph maps)
		// must be column names that exist in the term map's logical table."
		if (sqlConnection != null && contextLogicalTable != null 
				&& contextLogicalTable.getColumns(sqlConnection) != null
				&& termMap.getTemplate() != null
				&& termMap.getTemplate().getColumnNames() != null) {
			for (String columnName: termMap.getTemplate().getColumnNames()) {
				try {
					Identifier column = sqlConnection.vendor().parseIdentifiers(columnName, 1, 1)[0];
					if (!contextLogicalTable.getColumns(sqlConnection).contains(column)) {
						report.report(Problem.COLUMN_NOT_IN_LOGICAL_TABLE, 
								getContextResource(), RR.template, 
								ResourceFactory.createPlainLiteral(column.toString()));
					}
				} catch (IdentifierParseException ex) {
					report.report(Problem.INVALID_COLUMN_NAME, 
							getContextResource(), RR.template, 
							termMap.getTemplate(), 
							ex.getViolationType().name(), ex.getMessage());
				}
			}
		}

	}
	
	@Override
	public void visitComponent(TermMap termMap, Position position) {
		if (position != Position.SUBJECT_MAP) {
			if (!termMap.getClasses().isEmpty()) {
				report.report(Problem.ONLY_ACTIVE_ON_SUBJECT_MAP, 
						getContextResource(), RR.class_);
			}
			if (!termMap.getGraphs().isEmpty()) {
				report.report(Problem.ONLY_ACTIVE_ON_SUBJECT_MAP, 
						getContextResource(), RR.graph);
			}
			if (!termMap.getGraphMaps().isEmpty()) {
				report.report(Problem.ONLY_ACTIVE_ON_SUBJECT_MAP, 
						getContextResource(), RR.graphMap);
			}
		}
	}

	@Override
	public void visitComponent(PredicateObjectMap poMap) {
		if (poMap.getPredicateMaps().isEmpty() && poMap.getPredicates().isEmpty()) {
			report.report(Problem.REQUIRED_VALUE_MISSING,
					getContextResource(),
					new Property[]{RR.predicate, RR.predicateMap});
		}
		if (poMap.getObjectMaps().isEmpty() && poMap.getObjects().isEmpty()) { 
			report.report(Problem.REQUIRED_VALUE_MISSING,
					getContextResource(),
					new Property[]{RR.object, RR.objectMap});
		}
	}

	@Override
	public void visitComponent(ReferencingObjectMap referencingObjectMap) {
		require(referencingObjectMap, RR.parentTriplesMap);

		contextParentLogicalTable = null;
		if (referencingObjectMap.getParentTriplesMap() != null) {
			TriplesMap parentMap = mapping.triplesMaps().get(referencingObjectMap.getParentTriplesMap());
			if (getLogicalTable(parentMap) != null) {
				contextParentLogicalTable = getLogicalTable(parentMap);
			}
			
			//  "If the child query and parent query of a referencing object map 
			// are not identical, then the referencing object map must have 
			// at least one join condition."
			if (contextLogicalTable != null && contextLogicalTable.getEffectiveSQLQuery() != null
					&& contextParentLogicalTable != null && contextParentLogicalTable.getEffectiveSQLQuery() != null
					&& !contextLogicalTable.getEffectiveSQLQuery().equals(contextParentLogicalTable.getEffectiveSQLQuery())
					&& referencingObjectMap.getJoinConditions().isEmpty()) {
				report.report(Problem.JOIN_REQUIRED, 
						getContextResource(), RR.joinCondition);
			}
		}
	}

	@Override
	public void visitComponent(Join join) {
		require(join, RR.child);
		require(join, RR.parent);
		
		// "rr:child must be a column name that exists in the logical table of the triples map 
		// that contains the referencing object map"
		if (sqlConnection != null && contextLogicalTable != null 
				&& contextLogicalTable.getColumns(sqlConnection) != null
				&& join.getChild() != null) {
			Identifier child = join.getChild().asIdentifier(sqlConnection.vendor());
			if (child != null && !contextLogicalTable.getColumns(sqlConnection).contains(child)) {
				report.report(Problem.COLUMN_NOT_IN_LOGICAL_TABLE, 
						getContextResource(), RR.child, join.getChild());
			}
		}
		// "rr:parent must be a column name that exists in the logical table of the
		// referencing object map's parent triples map"
		if (sqlConnection != null && contextParentLogicalTable != null 
				&& contextParentLogicalTable.getColumns(sqlConnection) != null
				&& join.getParent() != null) {
			Identifier parent = join.getParent().asIdentifier(sqlConnection.vendor());
			if (parent  != null && !contextParentLogicalTable.getColumns(sqlConnection).contains(parent)) {
				report.report(Problem.COLUMN_NOT_IN_LOGICAL_TABLE, 
						getContextResource(), RR.parent, join.getParent());
			}
		}
	}

	@Override
	public void visitTerm(TableOrViewName tableName) {
		if (sqlConnection != null) {
			try {
				TableName parsed = TableName.create(
						sqlConnection.vendor().parseIdentifiers(tableName.toString(), 1, 3));
				if (!sqlConnection.isTable(parsed)) {
					report.report(Problem.NO_SUCH_TABLE_OR_VIEW, 
							getContextResource(), getContextProperty(),
							tableName);
				}
			} catch (IdentifierParseException ex) {
				String message = ex.getMessage();
				if (ex.getViolationType() == ViolationType.TOO_MANY_IDENTIFIERS) {
					message += "; must be in [catalog.][schema.]table form";
				}
				report.report(Problem.MALFORMED_TABLE_OR_VIEW_NAME, 
						getContextResource(), getContextProperty(),
						tableName,
						ex.getViolationType().name(), message);
			}
		}
	}
	

	/**
	 * "It must be valid to execute over the SQL connection. 
	 * The result of the query execution must not have duplicate column names. 
	 * Any columns in the SELECT list derived by projecting an expression 
	 * should be named, because otherwise they cannot be reliably referenced 
	 * in the rest of the mapping."
	 * 
	 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-sql-query">R2RML: SQL query</a> 
	 */
	@Override
	public void visitTerm(SQLQuery sqlQuery) {
		if (sqlConnection != null) {
			String error = sqlConnection.getParseError(sqlQuery.toString());
			if (error == null) {
				List<ColumnName> columns = sqlConnection.getSelectStatement(sqlQuery.toString()).getColumns();
				if (columns != null) {
					for (int i = 0; i < columns.size() - 1; i++) {
						for (int j = i + 1; j < columns.size(); j++) {
							if (columns.get(i).equals(columns.get(j))) {
								report.report(Problem.DUPLICATE_COLUMN_NAME_IN_SQL_QUERY,
										getContextResource(), getContextProperty(),
										sqlQuery, null, columns.get(i).getColumn().getName());
								break;
							}
						}
					}
				}
			} else {
				report.report(Problem.INVALID_SQL_QUERY, 
						getContextResource(), getContextProperty(),
						sqlQuery, null, error);
			}
		}
	}

	@Override
	public void visitTerm(ConstantShortcut shortcut, Position position) {
		if (position == Position.OBJECT_MAP) {
			if (shortcut.asTermMap().getConstant().isAnon()) {
				report.report(Problem.VALUE_MUST_BE_IRI_OR_LITERAL,
						getContextResource(), getContextProperty(),
						shortcut.asTermMap().getConstant());
			}
		} else if (!shortcut.asTermMap().getConstant().isURIResource()) {
			report.report(Problem.VALUE_MUST_BE_IRI, 
					getContextResource(), getContextProperty(), 
					shortcut.asTermMap().getConstant());
		}
	}
	
	@Override
	public void visitTerm(ColumnNameR2RML columnName) {
		try {
			if (sqlConnection != null) {
				sqlConnection.vendor().parseIdentifiers(columnName.toString(), 1, 1);
			}
		} catch (IdentifierParseException ex) {
			report.report(Problem.INVALID_COLUMN_NAME, 
					getContextResource(), getContextProperty(),
					columnName, 
					ex.getViolationType().name(), ex.getMessage());
		}
	}

	@Override
	public void visitTerm(StringTemplate stringTemplate) {
		if (stringTemplate.getSyntaxErrorCode() != null) {
			report.report(Problem.INVALID_STRING_TEMPLATE, 
					getContextResource(), getContextProperty(),
					stringTemplate, 
					stringTemplate.getSyntaxErrorCode(),
					stringTemplate.getSyntaxErrorMessage());
		} else if (stringTemplate.getColumnNames().length == 0) {
			report.report(Problem.STRING_TEMPLATE_WITHOUT_COLUMN_NAME, 
					getContextResource(), getContextProperty(),
					stringTemplate);
		} else {
			for (int i = 1; i < stringTemplate.getLiteralParts().length - 1; i++) {
				if (stringTemplate.getLiteralParts()[i].isEmpty()) {
					report.report(Problem.EMPTY_SEPARATOR_IN_STRING_TEMPLATE, 
							getContextResource(), getContextProperty(),
							stringTemplate);
					break;
				}
			}
		}
	}

	@Override
	public void visitTerm(ConstantIRI constantIRI) {
		if (!constantIRI.asJenaIRI().isAbsolute()) {
			report.report(Problem.IRI_NOT_ABSOLUTE, 
					getContextResource(), getContextProperty(),
					constantIRI);
			return;
		}
		Iterator<Violation> it = constantIRI.asJenaIRI().violations(true);
		while (it.hasNext()) {
			Violation violation = it.next();
			if (violation.isError()) {
				report.report(Problem.INVALID_IRI, 
						getContextResource(), getContextProperty(),
						constantIRI, 
						violation.codeName(), violation.getShortMessage());
			} else {
				report.report(Problem.IRI_WARNING, 
						getContextResource(), getContextProperty(),
						constantIRI, 
						violation.codeName(), violation.getShortMessage());
			}
		}
	}

	@Override
	public void visitTerm(LanguageTag languageTag) {
		// TODO: Use ARQ's LangTag.parse() instead? Would be suitable for RDF 1.1
		try {
			new com.hp.hpl.jena.rdf.arp.lang.LanguageTag(languageTag.toString());
		} catch (LanguageTagSyntaxException ex) {
			report.report(Problem.INVALID_LANGUAGE_TAG, 
					getContextResource(), getContextProperty(),
					languageTag, null, ex.getMessage());
		}
	}

	private void require(MappingComponent component, final Property requiredProperty) {
		component.accept(new MappingVisitor.DoNothingImplementation() {
			public void visitSimpleProperty(Property property, Object value) {
				if (requiredProperty.equals(property) && value == null) {
					report.report(Problem.REQUIRED_VALUE_MISSING, 
							getContextResource(), property);
				}
			}
			public void visitTermProperty(Property property, MappingTerm value) {
				if (requiredProperty.equals(property) && value == null) {
					report.report(Problem.REQUIRED_VALUE_MISSING, 
							getContextResource(), property);
				}
			}
			public void visitComponentProperty(Property property, Resource resource, 
					ComponentType... types) {
				if (requiredProperty.equals(property) && resource == null) {
					report.report(Problem.REQUIRED_VALUE_MISSING, 
							getContextResource(), property);
				}
			}
		});
	}
	
	private void validateComponentType(Property property, Resource resource, 
			ComponentType... types) {
		if (resource == null) return;
		List<Resource> foundTypes = new ArrayList<Resource>(types.length);
		List<Resource> supportedTypes = new ArrayList<Resource>(types.length);
		for (ComponentType type: types) {
			supportedTypes.add(type.asResource());
			MappingComponent value = mapping.getMappingComponent(resource, type);
			if (value != null) {
				foundTypes.add(type.asResource());
			}
		}
		if (foundTypes.isEmpty()) {
			report.report(Problem.VALUE_NOT_OF_EXPECTED_TYPE, getContextResource(), property, 
					supportedTypes.toArray(new Resource[supportedTypes.size()]));
		} else if (foundTypes.size() > 1) {
			report.report(Problem.CONFLICTING_TYPES, getContextResource(), property, 
					foundTypes.toArray(new Resource[foundTypes.size()]));
		}
	}
	
	private Resource getContextResource() {
		if (resourceStack.isEmpty()) return null;
		return resourceStack.peek();
	}
	
	private Property getContextProperty() {
		if (propertyStack.isEmpty()) return null;
		return propertyStack.peek();
	}
}
