package de.fuberlin.wiwiss.d2rq.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.csv.CSVParser;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Creates D2RQ domain classes (like {@link PropertyBridge},
 * {@link TranslationTable} from a Jena model representation
 * of a D2RQ mapping file. Checks the map for consistency.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MapParser.java,v 1.13 2006/09/10 22:18:45 cyganiak Exp $
 */
public class MapParser {

	/**
	 * Turns a relative URI into an absolute one, by using the
	 * current directory's <tt>file:</tt> URI as a base. This
	 * uses the same algorithm as Jena's Model class when reading
	 * a file.
	 * 
	 * @param uri Any URI
	 * @return An absolute URI corresponding to the input
	 */
	public static String absolutizeURI(String uri) {
		if (uri == null) {
			return null;
		}
		String n3 = "<> a 'foo' .";
		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(n3), uri, "N3");
		String absolute = m.listStatements().nextStatement().getSubject().getURI();
		if (uri.indexOf("#") > -1) {
			absolute += uri.substring(uri.indexOf("#"));
		}
		return absolute;
	}
	
	private Log log = LogFactory.getLog(MapParser.class);
	private Model model;
	private Graph graph;
	private String baseURI;
	private Collection propertyBridges = new ArrayList();
	private Map nodesToDatabases = new HashMap();
	private Map nodesToClassMapSpecs = new HashMap();
	private Map nodesToTranslationTables = new HashMap();
	private Map processingInstructions = new HashMap();
	
	/**
	 * Constructs a new MapParser from a Jena model containing the RDF statements
	 * from a D2RQ mapping file.
	 * @param mapModel a Jena model containing the RDF statements from a D2RQ mapping file
	 */
	public MapParser(Model mapModel, String baseURI) {
		this.model = mapModel;
		this.graph = mapModel.getGraph();
		this.baseURI = absolutizeURI(baseURI);
	}
	
	/**
	 * Starts the parsing process. Must be called before results can be retrieved
	 * from the getter methods.
	 */
	public void parse() {
	    parseProcessingInstructions();
		parseDatabases();
		parseClassMaps();
		parsePropertyBridges();
		parseAdditionalProperties();
		parseClassMapTypes();
		checkColumnTypes();
	}

	public Collection getDatabases() {
		return this.nodesToDatabases.values();
	}
	
	public Collection getPropertyBridges() {
		return this.propertyBridges;
	}
	
	public Map getProcessingInstructions() {
	    return this.processingInstructions;
	}
	
	private void parseProcessingInstructions() {
		Iterator it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.ProcessingInstructions.asNode());
		while (it.hasNext()) {
			Node instructions = ((Triple) it.next()).getSubject();
			// predicate is key, object is value => false parameter (XML style)
			processingInstructions.putAll(findLiteralsAsMap(instructions, Node.ANY, null, false,false));
		}
	}
	
	private void parseDatabases() {
	    ExtendedIterator it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.Database.asNode());
	    if (!it.hasNext()) {
	    	throw new D2RQException("No d2rq:Database defined in the mapping file.");
	    }
		while (it.hasNext()) {
			Node dbNode = ((Triple) it.next()).getSubject();
			this.nodesToDatabases.put(dbNode, buildDatabase(dbNode));
		}
	}
	
	private Database databaseForNode(Node node) {
		return (Database) this.nodesToDatabases.get(node);
	}

	private static Map d2rqColumnTypeToDatabaseColumnType;
	
	private Database buildDatabase(Node node) {
		String odbcDSN = findZeroOrOneLiteral(node, D2RQ.odbcDSN.asNode());
		String jdbcDSN = findZeroOrOneLiteral(node, D2RQ.jdbcDSN.asNode());
		String jdbcDriver = findZeroOrOneLiteral(node, D2RQ.jdbcDriver.asNode());
		String username = findZeroOrOneLiteral(node, D2RQ.username.asNode());
		String password = findZeroOrOneLiteral(node, D2RQ.password.asNode());
		String allowDistinct = findZeroOrOneLiteral(node, D2RQ.allowDistinct.asNode());
		String expressionTranslator = findZeroOrOneLiteral(node, D2RQ.expressionTranslator.asNode());
		
		if (d2rqColumnTypeToDatabaseColumnType==null) {
		    d2rqColumnTypeToDatabaseColumnType=new HashMap();
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.textColumn.asNode(),Database.textColumn);
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.numericColumn.asNode(),Database.numericColumn);
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.dateColumn.asNode(),Database.dateColumn);		    
		}
		Map columnTypes = new HashMap();
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.textColumn.asNode(), d2rqColumnTypeToDatabaseColumnType));
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.numericColumn.asNode(), d2rqColumnTypeToDatabaseColumnType));
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.dateColumn.asNode(), d2rqColumnTypeToDatabaseColumnType));
		if (jdbcDSN != null && jdbcDriver == null || jdbcDSN == null && jdbcDriver != null) {
			throw new D2RQException("d2rq:jdbcDSN and d2rq:jdbcDriver must be used together");
		}
		Database db = new Database(odbcDSN, jdbcDSN, jdbcDriver, username, password, columnTypes);
		if (allowDistinct!=null) {
		    if (allowDistinct.equals("true"))
		        db.setAllowDistinct(true);
		    else if (allowDistinct.equals("false"))
		        db.setAllowDistinct(false);
		    else 
		    	throw new D2RQException("d2rq:allowDistinct value must be true or false");			
		}
		if (expressionTranslator!=null)
		    db.setExpressionTranslator(expressionTranslator);
		return db;
	}

	private void parseClassMaps() {
		ExtendedIterator it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.ClassMap.asNode());
		while (it.hasNext()) {
			Node classMapNode = ((Triple) it.next()).getSubject();
			NodeMakerSpec spec = new NodeMakerSpec(toQName(classMapNode));
			fillClassMapSpec(spec, classMapNode);
			fillResourceSpec(spec, classMapNode, true);
			this.nodesToClassMapSpecs.put(classMapNode, spec);
		}
	}

	private NodeMakerSpec classMapSpecForNode(Node node) {
		return (NodeMakerSpec) this.nodesToClassMapSpecs.get(node);
	}

	private void fillClassMapSpec(NodeMakerSpec spec, Node classMapNode) {
		Node dbNode = findOneNode(classMapNode, D2RQ.dataStorage.asNode());
		spec.setDatabase(databaseForNode(dbNode));
	}
	
	public TranslationTable getTranslationTable(Node node) {
		if (this.nodesToTranslationTables.containsKey(node)) {
			return (TranslationTable) this.nodesToTranslationTables.get(node);
		}
		TranslationTable translationTable = new TranslationTable();
		String href = findZeroOrOneLiteralOrURI(node, D2RQ.href.asNode());
		if (href != null) {
			translationTable.addAll(new CSVParser(href).parse());
		}
		String className = findZeroOrOneLiteral(node, D2RQ.javaClass.asNode());
		if (className != null) {
			translationTable.setTranslatorClass(className, toResource(node));
		}
		ExtendedIterator it = this.graph.find(node, D2RQ.translation.asNode(), Node.ANY);
		if (href == null && className == null && !it.hasNext()) {
			this.log.warn("TranslationTable " + node + " contains no translations");
		}
		if (className != null && (href != null || it.hasNext())) {
			throw new D2RQException("Can't combine d2rq:javaClass with d2rq:translation or d2rq:href on " + node);
		}
		while (it.hasNext()) {
			Node translation = ((Triple) it.next()).getObject();
			String dbValue = findOneLiteral(translation, D2RQ.databaseValue.asNode());
			String rdfValue = findOneLiteralOrURI(translation, D2RQ.rdfValue.asNode());
			translationTable.addTranslation(dbValue, rdfValue);
		}
		this.nodesToTranslationTables.put(node, translationTable);
		return translationTable;
	}

	private void parsePropertyBridges() {
		Iterator it = this.graph.find(Node.ANY, D2RQ.belongsToClassMap.asNode(), Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			Node propBridgeNode = t.getSubject();
			Node classMapNode = t.getObject();
			parsePropertyBridge(propBridgeNode, classMapNode);
		}
	}
	
	private void parsePropertyBridge(Node bridgeNode, Node classMapNode) {
		NodeMakerSpec subjectSpec = classMapSpecForNode(classMapNode);
		if (subjectSpec == null) {
			throw new D2RQException("d2rq:belongsToClassMap for " +
					bridgeNode + " is no d2rq:ClassMap");
		}
		if (this.graph.find(bridgeNode, D2RQ.belongsToClassMap.asNode(), Node.ANY).toList().size() > 1) {
			throw new D2RQException("Multiple d2rq:belongsToClassMap in " + bridgeNode);
		}
		NodeMakerSpec objectSpec = new NodeMakerSpec(toQName(bridgeNode), subjectSpec);
		fillResourceSpec(objectSpec, bridgeNode, false);
		fillPropertyBridgeSpec(objectSpec, bridgeNode);
		Set propertiesForBridge = findPropertiesForBridge(bridgeNode);
		if (propertiesForBridge.isEmpty()) {
			throw new D2RQException("Missing d2rq:property for PropertyBridge " + bridgeNode);
		}
		Iterator it = propertiesForBridge.iterator();
		while (it.hasNext()) {
			Node property = (Node) it.next();
			createPropertyBridge(
					classMapNode,
					objectSpec.buildRelation(),
					subjectSpec.buildNodeMaker(),
					new FixedNodeMaker(property, false),
					objectSpec.buildNodeMaker());
		}
	}

	private void fillPropertyBridgeSpec(NodeMakerSpec spec, Node node) {
		Node refersTo = findZeroOrOneNode(node, D2RQ.refersToClassMap.asNode());
		if (refersTo != null) {
			NodeMakerSpec otherSpec = classMapSpecForNode(refersTo);
			if (otherSpec == null) {
				throw new D2RQException("d2rq:refersToClassMap of " + node + " is no valid d2rq:ClassMap");
			}
			spec.setRefersToClassMap(otherSpec);
		}
		String columnName = findZeroOrOneLiteral(node, D2RQ.column.asNode());
		if (columnName != null) {
			if (isObjectPropertyBridge(node)) {
				spec.setURIColumn(columnName);
			} else {
				spec.setLiteralColumn(columnName);
			}
		}
		String pattern = findZeroOrOneLiteral(node, D2RQ.pattern.asNode());
		if (pattern != null) {
			if (isObjectPropertyBridge(node)) {
				spec.setURIPattern(ensureIsAbsolute(pattern));
			} else {
				spec.setLiteralPattern(pattern);
			}
		}
		String datatype = findZeroOrOneLiteralOrURI(node, D2RQ.datatype.asNode());
		if (datatype != null) {
			spec.setDatatypeURI(datatype);
		}
		String lang = findZeroOrOneLiteral(node, D2RQ.lang.asNode());
		if (lang != null) {
			spec.setLang(lang);
		}
	}
	
	private void fillResourceSpec(NodeMakerSpec spec, Node node, boolean defaultToUnique) {
		String bNodeIdColumns = findZeroOrOneLiteral(node, D2RQ.bNodeIdColumns.asNode());
		if (bNodeIdColumns != null) {
			spec.setBlankColumns(bNodeIdColumns);
		}
		String uriColumnName = findZeroOrOneLiteral(node, D2RQ.uriColumn.asNode());
		if (uriColumnName != null) {
			spec.setURIColumn(uriColumnName);
		}
		String uriPattern = findZeroOrOneLiteral(node, D2RQ.uriPattern.asNode());
		if (uriPattern != null) {
			spec.setURIPattern(ensureIsAbsolute(uriPattern));
		}
		String valueRegex = findZeroOrOneLiteral(node, D2RQ.valueRegex.asNode());
		if (valueRegex != null) {
			spec.setRegexHint(valueRegex);
		}
		String valueContains = findZeroOrOneLiteral(node, D2RQ.valueContains.asNode());
		if (valueContains != null) {
			spec.setContainsHint(valueContains);
		}
		String valueMaxLength = findZeroOrOneLiteral(node, D2RQ.valueMaxLength.asNode());
		if (valueMaxLength != null) {
			try {
				int maxLength = Integer.parseInt(valueMaxLength);
				spec.setMaxLengthHint(maxLength);
			} catch (NumberFormatException nfex) {
				this.log.warn("Ignoring d2rq:valueMaxLength \"" +
						valueMaxLength + "\" on " + node + " (must be an integer)");
			}
		}
		Node translateWith = findZeroOrOneNode(node, D2RQ.translateWith.asNode());
		if (translateWith != null) {
			TranslationTable table = getTranslationTable(translateWith);
			if (table == null) {
				throw new D2RQException("Unknown d2rq:translateWith in " + node);
			}
			spec.setTranslationTable(table);
		}
		Iterator it = findLiterals(node, D2RQ.join.asNode()).iterator();
		while (it.hasNext()) {
			String join = (String) it.next();
			spec.addJoin(join);
		}
		it = findLiterals(node, D2RQ.condition.asNode()).iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			spec.addCondition(condition);
		}
		it = findLiterals(node, D2RQ.alias.asNode()).iterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			spec.addAlias(alias);
		}
		boolean isUnique = defaultToUnique;
		String containsDuplicates = findZeroOrOneLiteral(node, D2RQ.containsDuplicates.asNode());
		if ("true".equals(containsDuplicates)) {
			isUnique = false;
		} else if ("false".equals(containsDuplicates)) {
			isUnique = true;
		} else if (containsDuplicates != null) {
			throw new D2RQException("Illegal value '" + containsDuplicates + "' for d2rq:containsDuplicates on " + node);
		}
		if (isUnique) {
			spec.setIsUnique();
		}
	}
	
	private boolean isObjectPropertyBridge(Node node) {
		return this.graph.contains(node, RDF.Nodes.type, D2RQ.ObjectPropertyBridge.asNode());
	}

	private void createPropertyBridge(Node classMap, Relation relation, 
			NodeMaker subjects, NodeMaker predicates, NodeMaker objects) {
		PropertyBridge bridge = new PropertyBridge(relation, subjects, predicates, objects);
		this.propertyBridges.add(bridge);
		List bridgeList = (List) this.nodesToClassMapBridgeLists.get(classMap);
		if (bridgeList == null) {
			bridgeList = new ArrayList();
			this.nodesToClassMapBridgeLists.put(classMap, bridgeList);
		}
		bridgeList.add(bridge);
	}

	private Set findPropertiesForBridge(Node bridge) {
		Set results = new HashSet();
		Iterator it = this.graph.find(Node.ANY, D2RQ.propertyBridge.asNode(), bridge);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			results.add(t.getSubject());
		}
		it = this.graph.find(bridge, D2RQ.property.asNode(), Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			results.add(t.getObject());
		}
		return results;
	}

	private void parseAdditionalProperties() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.additionalProperty.asNode(), Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			NodeMakerSpec subjectSpec = classMapSpecForNode(t.getSubject());
			if (subjectSpec == null) {
				this.log.warn("Ignoring d2rq:additionalProperty on " +
						t.getSubject() + " as they are allowed only on d2rq:ClassMaps");
				continue;
			}
			createPropertyBridge(
					t.getSubject(),
					subjectSpec.buildRelation(),
					subjectSpec.buildNodeMaker(),
					new FixedNodeMaker(findOneNode(t.getObject(), D2RQ.propertyName.asNode()), false),
					new FixedNodeMaker(findOneNode(t.getObject(), D2RQ.propertyValue.asNode()), false));
		}
	}

	private void parseClassMapTypes() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.classMap.asNode(), Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			addRDFTypePropertyBridge(t.getObject(), t.getSubject());
		}
		it = this.graph.find(Node.ANY, D2RQ.class_.asNode(), Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			addRDFTypePropertyBridge(t.getSubject(), t.getObject());
		}
	}

	private void addRDFTypePropertyBridge(Node toClassMap, Node rdfsClass) {
		NodeMakerSpec spec = classMapSpecForNode(toClassMap);
		if (spec == null) {
			throw new D2RQException(toClassMap + ", referenced from " +
					rdfsClass + ", is no d2rq:ClassMap");
		}
		createPropertyBridge(toClassMap,
				spec.buildRelation(),
				spec.buildNodeMaker(),
				new FixedNodeMaker(RDF.Nodes.type, false),
				new FixedNodeMaker(rdfsClass, false));
	}

	private void assertHasColumnTypes(RDFRelation relation) {
		Iterator it = relation.projectionColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			relation.baseRelation().database().assertHasType(
					relation.baseRelation().aliases().originalOf(column));			
		}
	}

	private void checkColumnTypes() {
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			RDFRelation bridge = (RDFRelation) it.next();
			assertHasColumnTypes(bridge);
		}
	}
	
	private Resource toResource(Node node) {
		return this.model.getResource(node.getURI());
	}

	private String findZeroOrOneLiteral(Node subject, Node predicate) {
		Node node = findZeroOrOneNode(subject, predicate);
		if (node == null) {
			return null;
		}
		if (!node.isLiteral()) {
			throw new D2RQException(toQName(predicate) + " for " + subject + " must be literal");
		}
		return node.getLiteral().getLexicalForm();
	}

	private String findZeroOrOneLiteralOrURI(Node subject, Node predicate) {
		Node node = findZeroOrOneNode(subject, predicate);
		if (node == null) {
			return null;
		}
		if (node.isLiteral()) {
			return node.getLiteral().getLexicalForm();
		} else if (node.isURI()) {
			return node.getURI();
		}
		throw new D2RQException(toQName(predicate) + " for " + subject + " must be literal or URI");
	}

	private Node findZeroOrOneNode(Node subject, Node predicate) {
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		if (!it.hasNext()) {
			return null;
		}
		Node result = ((Triple) it.next()).getObject();
		if (it.hasNext()) {
			throw new D2RQException("Ignoring multiple " + toQName(predicate) + " on " + subject);
		}
		return result;
	}

	private Node findOneNode(Node subject, Node predicate) {
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		if (!it.hasNext()) {
			throw new D2RQException("Missing " + toQName(predicate) +
					" in " + toQName(subject));
		}
		Node result = ((Triple) it.next()).getObject();
		if (it.hasNext()) {
			this.log.warn("Ignoring multiple " + toQName(predicate) + " on " + subject);
		}
		return result;
	}

	private String findOneLiteral(Node subject, Node predicate) {
		Node node = findOneNode(subject, predicate);
		if (!node.isLiteral()) {
			throw new D2RQException(toQName(predicate) + " for " + subject + " must be literal");
		}
		return node.getLiteral().getLexicalForm();
	}

	private String findOneLiteralOrURI(Node subject, Node predicate) {
		Node node = findOneNode(subject, predicate);
		if (node.isLiteral()) {
			return node.getLiteral().getLexicalForm();
		} else if (node.isURI()) {
			return node.getURI();
		}
		throw new D2RQException(toQName(predicate) + " for " + subject + " must be literal or URI");
	}

	private Set findLiterals(Node subject, Node predicate) {
		Set result = new HashSet(3);
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		while (it.hasNext()) {
			Node node = ((Triple) it.next()).getObject();
			if (!node.isLiteral()) {
				throw new D2RQException(toQName(predicate) + " for " + subject + " must be literal");
			}
			result.add(node.getLiteral().getLexicalForm());
		}
		return result;
	}

	private Map findLiteralsAsMap(Node subject, Node predicate, Map predicateToObjectMap) {
	    return findLiteralsAsMap(subject, predicate, predicateToObjectMap, true, true);
	}
	private Map findLiteralsAsMap(Node subject, Node predicate, Map predicateToObjectMap, boolean objectIsKey, boolean warnIfNotLiteral) {
		Map result = new HashMap();
		ExtendedIterator itColText = this.graph.find(subject, predicate, Node.ANY);
		while (itColText.hasNext()) {
			Triple t = (Triple) itColText.next();
			subject=t.getSubject();
			predicate=t.getPredicate();
			Node object=t.getObject();
			if (!object.isLiteral()) {
			    if (warnIfNotLiteral) {
			        this.log.warn("Ignoring non-literal " + toQName(predicate) +
						" for " + subject + " (\"" + object + "\")");
			    }
				continue;
			}
			String objectString=object.getLiteral().getLexicalForm();
			Object predicateValue=(predicateToObjectMap==null)? predicate : predicateToObjectMap.get(predicate);
//			if (value==null) {
//			    throw new RuntimeException("Unmapped database type " + predicate);
//			}
			if (objectIsKey) // most cases
			    result.put(objectString, predicateValue); // put(key, value)
			else // xml style
			    result.put(predicateValue, objectString); 
		}
		return result;
	}

	private String toQName(Node node) {
		return PrettyPrinter.toString(node, this.model);
	}
	
	private String ensureIsAbsolute(String uriPattern) {
		if (uriPattern.indexOf(":") == -1) {
			return this.baseURI + uriPattern;
		}
		return uriPattern;
	}

	/**
	 * TODO This section was added as a quick hack for D2R Server 0.3 and should probably not be here
	 */
	private Map nodesToClassMapBridgeLists = new HashMap();
	private Map nodesToClassMapMakers = new HashMap();
	
	public Map propertyBridgesByClassMap() {
		return this.nodesToClassMapBridgeLists;
	}

	public Map NodeMakersByClassMap() {
		return this.nodesToClassMapMakers;
	}
}