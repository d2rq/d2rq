package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.r2rml.LogicalTable.BaseTableOrView;
import org.d2rq.r2rml.LogicalTable.R2RMLView;
import org.d2rq.r2rml.MappingComponent.ComponentType;
import org.d2rq.r2rml.TermMap.ColumnOrTemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.ColumnValuedTermMap;
import org.d2rq.r2rml.TermMap.ConstantValuedTermMap;
import org.d2rq.r2rml.TermMap.TemplateValuedTermMap;
import org.d2rq.r2rml.TermMap.TermType;
import org.d2rq.validation.Report;
import org.d2rq.validation.Message.Problem;
import org.d2rq.vocab.RR;
import org.d2rq.vocab.VocabularySummarizer;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;


/**
 * Creates an {@link R2RMLMapping} from a Jena model containing
 * an R2RML mapping graph.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class R2RMLReader {
	private final static Log log = LogFactory.getLog(R2RMLReader.class);

	public enum NodeType {
		ALL(null) {
			boolean isTypeOf(RDFNode node) { return true; }
			RDFNode coerce(RDFNode in) {
				return in;
			}
		},
		STRING_LITERAL(Problem.VALUE_MUST_BE_STRING_LITERAL) {
			boolean isTypeOf(RDFNode node) {
				if (!node.isLiteral()) return false;
				Literal l = node.asLiteral();
				return XSD.xstring.getURI().equals(l.getDatatypeURI())
						|| (l.getDatatypeURI() == null && "".equals(l.getLanguage()));
			}
			RDFNode coerce(RDFNode in) {
				if (in.isAnon()) return null;
				if (in.isURIResource()) {
					return ResourceFactory.createPlainLiteral(in.asResource().getURI());
				}
				return ResourceFactory.createPlainLiteral(in.asLiteral().getLexicalForm());
			}
		},
		IRI(Problem.VALUE_MUST_BE_IRI) {
			boolean isTypeOf(RDFNode node) {
				return (node.isURIResource());
			}
			RDFNode coerce(RDFNode in) {
				return in.isURIResource() ? in : null;
			}
		},
		IRI_OR_LITERAL(Problem.VALUE_MUST_BE_IRI_OR_LITERAL) {
			boolean isTypeOf(RDFNode node) {
				return (node.isURIResource() || node.isLiteral());
			}
			RDFNode coerce(RDFNode in) {
				return in.isAnon() ? null : in;
			}
		},
		IRI_OR_BLANK(Problem.VALUE_MUST_BE_IRI_OR_BLANK_NODE) {
			boolean isTypeOf(RDFNode node) {
				return (node.isURIResource() || node.isAnon());
			}
			RDFNode coerce(RDFNode in) {
				return in.isLiteral() ? ResourceFactory.createResource() : in;
			}
		};
		private final Problem ifNot;
		NodeType(Problem ifNot) { this.ifNot = ifNot; }
		abstract boolean isTypeOf(RDFNode node);
		abstract RDFNode coerce(RDFNode in);
	}

	private final Model model;
	private final String baseIRI;
	private Mapping mapping;
	private Report report = new Report();
	private boolean done = false;
	private Model remainingTriples;
	
	/**
	 * Constructs a new R2RMLMapParser from a Jena model
	 * containing an R2RML mapping graph
	 * @param baseIRI Used for resolving relative IRI templates
	 */
	public R2RMLReader(Model mappingModel, String baseIRI) {
		model = mappingModel;
		this.baseIRI = baseIRI;
	}
	
	/**
	 * @return <code>null</code> if no R2RML triples could be read
	 */
	public Mapping getMapping() {
		if (!done) {
			readMapping();
		}
		return mapping;
	}
	
	public void setReport(Report report) {
		this.report = report;
	}
	
	public Report getReport() {
		return report;
	}
	
	private void readMapping() {
		if (model.isEmpty()) {
			report.report(Problem.NO_TRIPLES);
			return;
		}

		VocabularySummarizer vocabulary = new VocabularySummarizer(RR.class);
		if (!vocabulary.usesVocabulary(model)) {
			report.report(Problem.NO_R2RML_TRIPLES);
			return;
		}

		mapping = new Mapping(baseIRI);
		copyPrefixes();
		remainingTriples = vocabulary.triplesInvolvingVocabulary(model);
		for (Resource class_: vocabulary.getUndefinedClasses(model)) {
			report.report(Problem.UNKNOWN_CLASS_IN_R2RML_NAMESPACE, class_);
		}
		for (Property property: vocabulary.getUndefinedProperties(model)) {
			report.report(Problem.UNKNOWN_PROPERTY_IN_R2RML_NAMESPACE, property);
		}

		for (Resource r: listResourcesWith(RR.logicalTable, RR.subjectMap, 
				RR.subject, RR.predicateObjectMap)) {
			mapping.triplesMaps().put(r, createTriplesMap(r));
		}

		ConflictChecker logicalTableConflictChecker = new ConflictChecker(report);
		for (Resource r: listResourcesWith(RR.tableName)) {
			logicalTableConflictChecker.add(r, RR.tableName);
			mapping.logicalTables().put(r, createBaseTableOrView(r));
		}
		for (Resource r: listResourcesWith(RR.sqlQuery)) {
			logicalTableConflictChecker.add(r, RR.sqlQuery);
			mapping.logicalTables().put(r, createR2RMLView(r));
		}
		ConflictChecker termMapConflictChecker = new ConflictChecker(report);
		for (Resource r: listResourcesWith(RR.constant)) {
			termMapConflictChecker.add(r, RR.constant);
			mapping.termMaps().put(r, createConstantValuedTermMap(r));
		}
		for (Resource r: listResourcesWith(RR.column)) {
			termMapConflictChecker.add(r, RR.column);
			mapping.termMaps().put(r, createColumnValuedTermMap(r));
		}
		for (Resource r: listResourcesWith(RR.template)) {
			termMapConflictChecker.add(r, RR.template);
			mapping.termMaps().put(r, createTemplateValuedTermMap(r));
		}
		for (Resource r: listResourcesWith(RR.predicate, RR.predicateMap, RR.object, RR.objectMap)) {
			PredicateObjectMap poMap = createPredicateObjectMap(r);
			mapping.predicateObjectMaps().put(r, poMap);
		}
		for (Resource r: listResourcesWith(RR.parentTriplesMap)) {
			mapping.referencingObjectMaps().put(r, createReferencingObjectMap(r));
		}
		for (Resource r: listResourcesWith(RR.child, RR.parent)) {
			mapping.joins().put(r, createJoin(r));
		}
		
		checkForSpuriousTypes();
		checkForSpuriousTriples();
		
		log.info("Done reading R2RML map with " + 
				mapping.triplesMaps().size() + " rr:TriplesMaps");
	}
	
	/**
	 * Copies all prefixes from the mapping file Model to the R2RML mapping.
	 * Administrative R2RML prefixes are dropped on the assumption that they
	 * are not wanted in the actual data.
	 */ 
	private void copyPrefixes() {
		mapping.getPrefixMapping().setNsPrefixes(model);
		Iterator<Map.Entry<String, String>> it = 
			mapping.getPrefixMapping().getNsPrefixMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry<String,String> entry = it.next();
			String namespace = entry.getValue();
			if (RR.NS.equals(namespace) && "rr".equals(entry.getKey())) {
				mapping.getPrefixMapping().removeNsPrefix(entry.getKey());
			}
		}
	}

	private TriplesMap createTriplesMap(Resource r) {
		TriplesMap result = new TriplesMap();
		result.setLogicalTable(getResource(r, RR.logicalTable));
		result.setSubject(ConstantShortcut.create(getRDFNode(r, RR.subject)));
		result.setSubjectMap(getResource(r, RR.subjectMap));
		result.getPredicateObjectMaps().addAll(getResources(r, RR.predicateObjectMap));
		return result;
	}
	
	private LogicalTable createBaseTableOrView(Resource r) {
		if (model.contains(r, RDF.type, RR.R2RMLView)) {
			report.report(Problem.SPURIOUS_TYPE, r, RDF.type, RR.R2RMLView);
		}
		BaseTableOrView result = new BaseTableOrView();
		result.setTableName(TableOrViewName.create(getString(r, RR.tableName)));
		return result;
	}
	
	private LogicalTable createR2RMLView(Resource r) {
		if (model.contains(r, RDF.type, RR.BaseTableOrView)) {
			report.report(Problem.SPURIOUS_TYPE, r, RDF.type, RR.BaseTableOrView);
		}
		R2RMLView result = new R2RMLView();
		result.setSQLQuery(SQLQuery.create(getString(r, RR.sqlQuery)));
		for (RDFNode sqlVersion: getRDFNodes(r, RR.sqlVersion, NodeType.IRI)) {
			result.getSQLVersions().add(ConstantIRI.create(sqlVersion.asResource()));
		}
		return result;
	}
	
	private TermMap createConstantValuedTermMap(Resource r) {
		ConstantValuedTermMap result = new ConstantValuedTermMap();
		result.setConstant(getRDFNode(r, RR.constant));
		readTermMap(result, r);
		return result;
	}

	private TermMap createColumnValuedTermMap(Resource r) {
		ColumnValuedTermMap result = new ColumnValuedTermMap();
		result.setColumnName(ColumnNameR2RML.create(getString(r, RR.column)));
		readColumnOrTemplateValuedTermMap(result, r);
		return result;
	}

	private TermMap createTemplateValuedTermMap(Resource r) {
		TemplateValuedTermMap result = new TemplateValuedTermMap();
		result.setTemplate(StringTemplate.create(getString(r, RR.template)));
		readColumnOrTemplateValuedTermMap(result, r);
		return result;
	}

	private void readColumnOrTemplateValuedTermMap(ColumnOrTemplateValuedTermMap termMap, Resource r) {
		Resource termType = getIRIResource(r, RR.termType);
		if (termType != null) {
			if (TermType.getFor(termType) == null) {
				report.report(Problem.INVALID_TERM_TYPE, r, RR.termType, termType);
			} else {
				termMap.setSpecifiedTermType(TermType.getFor(termType));
			}
		}
		termMap.setDatatype(ConstantIRI.create(getIRIResource(r, RR.datatype)));
		termMap.setLanguageTag(LanguageTag.create(getString(r, RR.language)));
		termMap.setInverseExpression(StringTemplate.create(getString(r, RR.inverseExpression)));
		readTermMap(termMap, r);
	}
	
	private void readTermMap(TermMap termMap, Resource r) {
		termMap.getGraphMaps().addAll(getResources(r, RR.graphMap));
		for (RDFNode graph: getRDFNodes(r, RR.graph)) {
			termMap.getGraphs().add(ConstantShortcut.create(graph));
		}
		for (RDFNode class_: getRDFNodes(r, RR.class_, NodeType.IRI)) {
			termMap.getClasses().add(ConstantIRI.create(class_.asResource()));
		}
		checkTermMapType(r, RR.SubjectMap, RR.subjectMap);
		checkTermMapType(r, RR.PredicateMap, RR.predicateMap);
		checkTermMapType(r, RR.ObjectMap, RR.objectMap);
		checkTermMapType(r, RR.GraphMap, RR.graphMap);
	}
	
	private void checkTermMapType(Resource r, Resource possibleType, Property requiredProperty) {
		if (!model.contains(r, RDF.type, possibleType)) return;
		if (!model.contains(null, requiredProperty, r)) {
			report.report(Problem.SPURIOUS_TYPE, r, RDF.type, possibleType);
		}
	}

	private PredicateObjectMap createPredicateObjectMap(Resource r) {
		PredicateObjectMap result = new PredicateObjectMap();
		result.getPredicateMaps().addAll(getResources(r, RR.predicateMap));
		for (RDFNode predicate: getRDFNodes(r, RR.predicate)) {
			result.getPredicates().add(ConstantShortcut.create(predicate));
		}
		result.getObjectMaps().addAll(getResources(r, RR.objectMap));
		for (RDFNode object: getRDFNodes(r, RR.object)) {
			result.getObjects().add(ConstantShortcut.create(object));
		}
		result.getGraphMaps().addAll(getResources(r, RR.graphMap));
		for (RDFNode graph: getRDFNodes(r, RR.graph)) {
			result.getGraphs().add(ConstantShortcut.create(graph));
		}
		return result;
	}
	
	private ReferencingObjectMap createReferencingObjectMap(Resource r) {
		ReferencingObjectMap result = new ReferencingObjectMap();
		result.setParentTriplesMap(getResource(r, RR.parentTriplesMap));
		result.getJoinConditions().addAll(getResources(r, RR.joinCondition));
		return result;
	}
	
	private Join createJoin(Resource r) {
		Join result = new Join();
		result.setChild(ColumnNameR2RML.create(getString(r, RR.child)));
		result.setParent(ColumnNameR2RML.create(getString(r, RR.parent)));
		return result;
	}
	
	public Set<Resource> listResourcesWith(Property... properties) {
		Set<Resource> result = new HashSet<Resource>();
		for (Property property: properties) {
			ResIterator it = model.listResourcesWithProperty(property);
			while (it.hasNext()) {
				result.add(it.next());
			}
		}
		return result;
	}

	private void checkForSpuriousTypes() {
		// This doesn't catch spurious subclass triples, e.g., rr:SubjectMap,
		// rr:R2RMLView.
		for (ComponentType type: ComponentType.values()) {
			ResIterator it = model.listResourcesWithProperty(RDF.type, type.asResource());
			while (it.hasNext()) {
				Resource r = it.next();
				if (mapping.getMappingComponent(r, type) == null) {
					report.report(Problem.SPURIOUS_TYPE, r, RDF.type, type.asResource());
				}
			}
		}
		remainingTriples.removeAll(null, RDF.type, (RDFNode) null);
	}
	
	private void checkForSpuriousTriples() {
		StmtIterator it = remainingTriples.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			report.report(Problem.SPURIOUS_TRIPLE, stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
		}
	}
	
	public List<RDFNode> getRDFNodes(Resource r, Property p, NodeType acceptableNodes) {
		List<RDFNode> result = new ArrayList<RDFNode>();
		StmtIterator it = r.listProperties(p);
		while (it.hasNext()) {
			Statement stmt = it.next();
			remainingTriples.remove(stmt);
			if (acceptableNodes.isTypeOf(stmt.getObject())) {
				result.add(stmt.getObject());
			} else {
				if (acceptableNodes.coerce(stmt.getObject()) != null) {
					result.add(acceptableNodes.coerce(stmt.getObject()));
				}
				report.report(acceptableNodes.ifNot, r, p, stmt.getObject());
			}
		}
		Collections.sort(result, RDFComparator.getRDFNodeComparator());
		return result;
	}
	
	public RDFNode getRDFNode(Resource r, Property p, NodeType acceptableNodes) {
		List<RDFNode> all = getRDFNodes(r, p, acceptableNodes);
		if (all.isEmpty()) return null;
		if (all.size() > 1) {
			report.report(Problem.DUPLICATE_VALUE, r, p, all.toArray(new RDFNode[all.size()]));
		}
		return all.iterator().next();
	}
	
	public List<RDFNode> getRDFNodes(Resource r, Property p) {
		return getRDFNodes(r, p, NodeType.ALL);
	}
	
	public RDFNode getRDFNode(Resource r, Property p) {
		return getRDFNode(r, p, NodeType.ALL);
	}
	
	public List<Resource> getResources(Resource r, Property p, NodeType acceptableNodeTypes) {
		List<Resource> result = new ArrayList<Resource>();
		for (RDFNode node: getRDFNodes(r, p, acceptableNodeTypes)) {
			result.add(node.asResource());
		}
		return result;
	}
	
	public Resource getResource(Resource r, Property p, NodeType acceptableNodeTypes) {
		return (Resource) getRDFNode(r, p, acceptableNodeTypes);
	}

	public List<Resource> getResources(Resource r, Property p) {
		return getResources(r, p, NodeType.IRI_OR_BLANK);
	}
	
	public Resource getResource(Resource r, Property p) {
		return getResource(r, p, NodeType.IRI_OR_BLANK);
	}

	public List<Resource> getIRIResources(Resource r, Property p) {
		return getResources(r, p, NodeType.IRI);
	}
	
	public Resource getIRIResource(Resource r, Property p) {
		return getResource(r, p, NodeType.IRI);
	}

	public List<String> getStrings(Resource r, Property p) {
		List<String> result = new ArrayList<String>();
		for (RDFNode node: getRDFNodes(r, p, NodeType.STRING_LITERAL)) {
			result.add(node.asLiteral().getLexicalForm());
		}
		return result;
	}
	
	public String getString(Resource r, Property p) {
		RDFNode node = getRDFNode(r, p, NodeType.STRING_LITERAL);
		if (node == null) return null;
		return node.asLiteral().getLexicalForm();
	}
}