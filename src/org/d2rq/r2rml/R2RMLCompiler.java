package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.CompiledMapping;
import org.d2rq.D2RQException;
import org.d2rq.D2RQOptions;
import org.d2rq.ResourceCollection;
import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.nodes.TypedNodeMaker.NodeType;
import org.d2rq.r2rml.LogicalTable.BaseTableOrView;
import org.d2rq.r2rml.LogicalTable.R2RMLView;
import org.d2rq.r2rml.TermMap.ColumnOrTemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.ColumnValuedTermMap;
import org.d2rq.r2rml.TermMap.ConstantValuedTermMap;
import org.d2rq.r2rml.TermMap.Position;
import org.d2rq.r2rml.TermMap.TemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.TermType;
import org.d2rq.values.BaseIRIValueMaker;
import org.d2rq.values.ColumnValueMaker;
import org.d2rq.values.TemplateValueMaker;
import org.d2rq.values.TemplateValueMaker.ColumnFunction;
import org.d2rq.values.ValueMaker;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;


public class R2RMLCompiler implements CompiledMapping {
	public final static Log log = LogFactory.getLog(R2RMLCompiler.class);

	private final Mapping mapping;
	private final SQLConnection sqlConnection;
	private final Collection<TripleRelation> tripleRelations = 
		new ArrayList<TripleRelation>();
	private final Map<String,ResourceCollection> resourceCollections =
		new HashMap<String,ResourceCollection>();
	private Resource currentTriplesMapResource;
	private final Collection<TripleRelation> currentTripleRelations = 
		new ArrayList<TripleRelation>();
	private NamedOp currentTable = null;
	private NodeMaker subjectMaker = null;
	private boolean fastMode = false;
	private boolean compiled = false;
	
	public R2RMLCompiler(Mapping mapping, SQLConnection sqlConnection) {
		this.mapping = mapping;
		this.sqlConnection = sqlConnection;
	}

	public void connect() {
		sqlConnection.connection();
	}
	
	public void close() {
		sqlConnection.close();
	}
	
	public PrefixMapping getPrefixes() {
		return mapping.getPrefixMapping();
	}
	
	public Collection<TripleRelation> getTripleRelations() {
		checkCompiled();
		return tripleRelations;
	}
	
	public Collection<? extends DownloadRelation> getDownloadRelations() {
		return Collections.emptySet();
	}

	public Collection<SQLConnection> getSQLConnections() {
		return Collections.singleton(sqlConnection);
	}

	public List<String> getResourceCollectionNames() {
		checkCompiled();
		List<String> result = new ArrayList<String>(resourceCollections.keySet());
		Collections.sort(result);
		return result;
	}

	public List<String> getResourceCollectionNames(Node forNode) {
		checkCompiled();
		List<String> result = new ArrayList<String>();
		for (String name: resourceCollections.keySet()) {
			if (resourceCollections.get(name).mayContain(forNode)) {
				result.add(name);
			}
		}
		Collections.sort(result);
		return result;
	}

	public ResourceCollection getResourceCollection(String name) {
		checkCompiled();
		return resourceCollections.get(name);
	}

	public Graph getAdditionalTriples() {
		return GraphFactory.createPlainGraph();
	}

	public void setFastMode(boolean fastMode) {
		this.fastMode = fastMode;
	}
	
	public Context getContext() {
		return D2RQOptions.getContext(fastMode);
	}
	
	private void checkCompiled() {
		if (!compiled) {
			compiled = true;
			visitComponent(mapping);
		}
	}
	
	private void visitComponent(Mapping mapping) {
		for (Resource r: mapping.triplesMaps().resources()) {
			currentTriplesMapResource = r;
			visitComponent(mapping.triplesMaps().get(r));
		}
	}
	
	private void visitComponent(TriplesMap triplesMap) {
		LogicalTable table = mapping.logicalTables().get(
				triplesMap.getLogicalTable());
		table.accept(new RelationCompiler());
		TermMap subjectMap = triplesMap.getSubject() != null
				? triplesMap.getSubject().asTermMap()
				: mapping.termMaps().get(triplesMap.getSubjectMap());
				subjectMaker = createNodeMaker(subjectMap, Position.SUBJECT_MAP);
		for (PredicateObjectMap poMap: mapping.predicateObjectMaps().getAll(
				triplesMap.getPredicateObjectMaps())) {
			visitComponent(poMap);
		}
		for (ConstantIRI classIRI: subjectMap.getClasses()) {
			currentTripleRelations.add(createTripleRelation(
					new FixedNodeMaker(RDF.type.asNode()),
					new FixedNodeMaker(Node.createURI(classIRI.toString()))));
		}
		if (subjectMaker == null) {
			// TODO: Remove this check once we handle all term map types
			log.warn("null subject map");
		} else {
			resourceCollections.put(getTriplesMapName(), 
					new ResourceCollection(this, sqlConnection, subjectMaker, 
							createTabular(subjectMaker.projectionSpecs()), 
							currentTripleRelations));
		}
		tripleRelations.addAll(currentTripleRelations);
		currentTripleRelations.clear();
	}
	
	private void visitComponent(PredicateObjectMap poMap) {
		Collection<TermMap> predicateMaps = new ArrayList<TermMap>();
		for (ConstantShortcut predicates: poMap.getPredicates()) {
			predicateMaps.add(predicates.asTermMap());
		}
		predicateMaps.addAll(mapping.termMaps().getAll(poMap.getPredicateMaps()));
		for (TermMap predicateMap: predicateMaps) {
			NodeMaker predicateMaker = createNodeMaker(predicateMap, Position.PREDICATE_MAP);
			Collection<TermMap> objectMaps = new ArrayList<TermMap>();
			for (ConstantShortcut objects: poMap.getObjects()) {
				objectMaps.add(objects.asTermMap());
			}
			objectMaps.addAll(mapping.termMaps().getAll(poMap.getObjectMaps()));
			if (!mapping.referencingObjectMaps().getAll(poMap.getObjectMaps()).isEmpty()) {
				// FIXME: Implement rr:ReferencingObjectMap
				throw new D2RQException("rr:ReferencingObjectMap not yet imlemented", D2RQException.NOT_YET_IMPLEMENTED);
			}
			for (TermMap objectMap: objectMaps) {
				NodeMaker objectMaker = createNodeMaker(objectMap, Position.OBJECT_MAP);
				currentTripleRelations.add(createTripleRelation(predicateMaker, objectMaker));
			}
		}
	}
	
	private TripleRelation createTripleRelation(NodeMaker predicateMaker,
			NodeMaker objectMaker) {
		if (subjectMaker == null || predicateMaker == null || objectMaker == null) {
			// TODO: Remove this check once we handle all term map types
			log.warn("null term map");
			return null;
		}
		Set<ProjectionSpec> columns = new HashSet<ProjectionSpec>();
		columns.addAll(subjectMaker.projectionSpecs());
		columns.addAll(predicateMaker.projectionSpecs());
		columns.addAll(objectMaker.projectionSpecs());
		return new TripleRelation(sqlConnection ,createTabular(columns), 
				subjectMaker, predicateMaker, objectMaker);
	}
	
	private DatabaseOp createTabular(Set<ProjectionSpec> columns) {
		DatabaseOp op = currentTable;
		ProjectOp.create(op, columns);
		return op;
	}
	
	private NodeMaker createNodeMaker(TermMap termMap, Position position) {
		NodeMakerCompiler compiler = new NodeMakerCompiler();
		termMap.acceptAs(compiler, position);
		return compiler.result;
	}
	
	private TemplateValueMaker toTemplate(StringTemplate template, TermType termType) {
		String[] literalParts = template.getLiteralParts().clone();
		if (termType == TermType.IRI && !literalParts[0].matches("[a-zA-Z][a-zA-Z0-9.+-]*:.*")) {
			literalParts[0] = mapping.getBaseIRI() + literalParts[0];
		}
		ColumnName[] qualifiedColumns = new ColumnName[template.getColumnNames().length];
		ColumnFunction[] functions = new ColumnFunction[template.getColumnNames().length];
		for (int i = 0; i < qualifiedColumns.length; i++) {
			qualifiedColumns[i] = ColumnName.create(currentTable.getTableName(), 
					template.getColumnNames()[i]);
			functions[i] = termType == TermType.IRI ? TemplateValueMaker.ENCODE : TemplateValueMaker.IDENTITY;
		}
		return new TemplateValueMaker(literalParts, qualifiedColumns, functions);
	}
	
	private String getTriplesMapName() {
		if (currentTriplesMapResource.isAnon()) {
			return currentTriplesMapResource.toString();
		}
		return currentTriplesMapResource.getLocalName();
	}
	
	private class RelationCompiler extends MappingVisitor.DoNothingImplementation {
		@Override
		public void visitComponent(BaseTableOrView table) {
			currentTable = sqlConnection.getTable(table.getTableName().asQualifiedTableName());
		}
		@Override
		public void visitComponent(R2RMLView query) {
			String sql = query.getSQLQuery().toString();
			String name = "VIEW" + Integer.toHexString(sql.hashCode());
			SQLOp selectStatement = sqlConnection.getSelectStatement(sql);
			currentTable = AliasOp.create(selectStatement, name);
		}
	}
	
	private class NodeMakerCompiler extends MappingVisitor.DoNothingImplementation {
		private NodeMaker result;
		@Override
		public void visitComponent(ConstantValuedTermMap termMap, Position position) {
			result = new FixedNodeMaker(termMap.getConstant().asNode());
		}
		public void visitComponent(ColumnValuedTermMap termMap, Position position) {
			ColumnName qualified = ColumnName.create(currentTable.getTableName(), 
					termMap.getColumnName().asIdentifier());
			NodeType nodeType = getNodeType(termMap, position, currentTable.getColumnType(qualified));
			ValueMaker baseValueMaker = new ColumnValueMaker(qualified);
			if (nodeType == TypedNodeMaker.URI) {
				baseValueMaker = new BaseIRIValueMaker(mapping.getBaseIRI(), baseValueMaker);
			}
			result = new TypedNodeMaker(nodeType, baseValueMaker);
		}
		public void visitComponent(TemplateValuedTermMap termMap, Position position) {
			TemplateValueMaker pattern = toTemplate(termMap.getTemplate(), 
					termMap.getTermType(position));
			DataType characterType = GenericType.CHARACTER.dataTypeFor(sqlConnection.vendor());
			result = new TypedNodeMaker(getNodeType(termMap, position, characterType), pattern);
		}
		private NodeType getNodeType(ColumnOrTemplateValuedTermMap termMap, Position position, DataType naturalType) {
			if (termMap.getTermType(position) == TermType.IRI) {
				return TypedNodeMaker.URI;
			}
			if (termMap.getTermType(position) == TermType.BLANK_NODE) {
				return TypedNodeMaker.BLANK;
			}
			if (termMap.getLanguageTag() != null) {
				return TypedNodeMaker.languageLiteral(
						termMap.getLanguageTag().toString());
			}
			if (termMap.getDatatype() != null) {
				return TypedNodeMaker.typedLiteral(
						TypeMapper.getInstance().getSafeTypeByName(termMap.getDatatype().toString()));
			}
			if (!XSD.xstring.getURI().equals(naturalType.rdfType())) {
				return TypedNodeMaker.typedLiteral(TypeMapper.getInstance().getSafeTypeByName(naturalType.rdfType()));
			}
			return TypedNodeMaker.PLAIN_LITERAL;
		}
	}
}
