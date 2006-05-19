/*
 * $Id: MapParser.java,v 1.1 2006/05/19 19:13:02 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.helpers.CSVParser;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ContainsRestriction;
import de.fuberlin.wiwiss.d2rq.map.D2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.LiteralMaker;
import de.fuberlin.wiwiss.d2rq.map.MaxLengthRestriction;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.RegexRestriction;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.map.URIMatchPolicy;
import de.fuberlin.wiwiss.d2rq.map.UriMaker;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;

/**
 * Creates D2RQ domain classes (like {@link PropertyBridge},
 * {@link TranslationTable} from a Jena model representation
 * of a D2RQ mapping file. Checks the map for consistency.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MapParser.java,v 1.1 2006/05/19 19:13:02 cyganiak Exp $
 */
public class MapParser {
	private Model model;
	private Graph graph;
	private Map databases = new HashMap();
	private Map classMaps = new HashMap();
	private Map translationTables = new HashMap();
	private Map propertyBridges = new HashMap();
	private Map resourcesConditionsMap = new HashMap();
	private Map resourcesDatabasesMap = new HashMap();
	private Set columnResourceMakers = new HashSet();
	private Set patternResourceMakers = new HashSet();
	private Set uniqueNodeMakers = new HashSet();
	private PrefixMapping prefixes;
	private Map processingInstructions = new HashMap();
	
	/**
	 * Constructs a new MapParser from a Jena model containing the RDF statements
	 * from a D2RQ mapping file.
	 * @param mapModel a Jena model containing the RDF statements from a D2RQ mapping file
	 */
	public MapParser(Model mapModel) {
		this.model = mapModel;
		this.graph = mapModel.getGraph();
		this.prefixes = new PrefixMappingImpl();
		this.prefixes.setNsPrefix("d2rq", D2RQ.uri);
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
		addRDFTypePropertyBridges();
	}

	public Collection getDatabases() {
		return this.databases.values();
	}
	
	public Collection getClassMaps() {
		return this.classMaps.values();
	}

	public Collection getPropertyBridges() {
		return this.propertyBridges.values();
	}
	
	public Map getProcessingInstructions() {
	    return this.processingInstructions;
	}
	
	private void parseProcessingInstructions() {
	    ExtendedIterator it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.ProcessingInstructions);
	    while (it.hasNext()) {
	        Node instructions=((Triple) it.next()).getSubject();
	        // predicate is key, object is value => false parameter (XML style)
	        Map nextMap=findLiteralsAsMap(instructions, Node.ANY, null, false,false); 		    
	        processingInstructions.putAll(nextMap);
	    }
	}
	

	private void parseDatabases() {
	    ExtendedIterator it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.Database);
	    if (!it.hasNext()) {
	        Logger.instance().error("No d2rq:Database defined in the mapping file.");
	        return;
	    }
		while (it.hasNext()) {
			Node database = ((Triple) it.next()).getSubject();
			buildDatabase(database);
		}
	}
	
	private static Map d2rqColumnTypeToDatabaseColumnType;
	
	private void buildDatabase(Node node) {
		String odbcDSN = findZeroOrOneLiteral(node, D2RQ.odbcDSN);
		String jdbcDSN = findZeroOrOneLiteral(node, D2RQ.jdbcDSN);
		String jdbcDriver = findZeroOrOneLiteral(node, D2RQ.jdbcDriver);
		String username = findZeroOrOneLiteral(node, D2RQ.username);
		String password = findZeroOrOneLiteral(node, D2RQ.password);
		String allowDistinct = findZeroOrOneLiteral(node, D2RQ.allowDistinct);
		String expressionTranslator = findZeroOrOneLiteral(node, D2RQ.expressionTranslator);
		
		if (d2rqColumnTypeToDatabaseColumnType==null) {
		    d2rqColumnTypeToDatabaseColumnType=new HashMap();
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.textColumn,Database.textColumn);
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.numericColumn,Database.numericColumn);
		    d2rqColumnTypeToDatabaseColumnType.put(D2RQ.dateColumn,Database.dateColumn);		    
		}
		Map columnTypes = new HashMap();
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.textColumn, d2rqColumnTypeToDatabaseColumnType));
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.numericColumn, d2rqColumnTypeToDatabaseColumnType));
		columnTypes.putAll(findLiteralsAsMap(node, D2RQ.dateColumn, d2rqColumnTypeToDatabaseColumnType));
		if (jdbcDSN != null && jdbcDriver == null || jdbcDSN == null && jdbcDriver != null) {
			Logger.instance().error("d2rq:jdbcDSN and d2rq:jdbcDriver must be used together");
		}
		Database db = new Database(odbcDSN, jdbcDSN, jdbcDriver, username, password, columnTypes);
		if (allowDistinct!=null) {
		    if (allowDistinct.equals("true"))
		        db.setAllowDistinct(true);
		    else if (allowDistinct.equals("false"))
		        db.setAllowDistinct(false);
		    else 
		        Logger.instance().error("d2rq:allowDistinct value must be true or false");			
		}
		if (expressionTranslator!=null)
		    db.setExpressionTranslator(expressionTranslator);
		this.databases.put(node, db);
	}

	private void parseClassMaps() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.dataStorage, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			Database db = (Database) this.databases.get(t.getObject());
			if (db == null) {
				Logger.instance().error("Unknown d2rq:dataStorage for d2rq:ClassMap " +
						t.getSubject());
			}
			buildClassMap(t.getSubject(), db);
		}
		it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.ClassMap);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			if (this.classMaps.get(t.getSubject()) == null) {
				Logger.instance().error("Missing d2rq:dataStorage for d2rq:ClassMap " +
						t.getSubject());
			}
		}
	}

	private void buildClassMap(Node node, Database db) {
		if (this.classMaps.containsKey(node)) {
			return;
		}
		String pattern = findZeroOrOneLiteral(node, D2RQ.uriPattern);
		String columnName = findZeroOrOneLiteral(node, D2RQ.uriColumn);
		String bNodeColumns = findZeroOrOneLiteral(node, D2RQ.bNodeIdColumns);
		ValueSource valueSource = null;
		if (pattern != null) {
			valueSource = new Pattern(pattern);
		} else if (columnName != null) {
			if (valueSource != null) {
				Logger.instance().error("Cannot combine d2rq:uriPattern and d2rq:uriColumn on " + node);
				return;
			}
			valueSource = new Column(columnName);
		} else if (bNodeColumns != null) {
			if (valueSource != null) {
				Logger.instance().error("Cannot combine d2rq:uri* and d2rq:bNodeIdColumns on " + node);
				return;
			}
			valueSource = new BlankNodeIdentifier(bNodeColumns, node.toString());
		} else {
			Logger.instance().error("The classMap " + node +
					" needs a d2rq:uriColumn, d2rq:uriPattern or d2rq:bNodeIdColumns");
			return;			
		}
		Node translateWith = findZeroOrOneNode(node, D2RQ.translateWith);
		if (translateWith != null) {
			TranslationTable table = getTranslationTable(translateWith);
			if (table == null) {
				Logger.instance().error("Unknown d2rq:translateWith in " + node);
			}
			valueSource = table.getTranslatingValueSource(valueSource);
		}
		NodeMaker resourceMaker = (bNodeColumns != null) ?
				(NodeMaker) new BlankNodeMaker(node.toString(), valueSource) :
				(NodeMaker) new UriMaker(node.toString(), valueSource);
		assertHasColumnTypes(resourceMaker, db);
		this.classMaps.put(node, resourceMaker);
		this.resourcesDatabasesMap.put(resourceMaker, db);
		if (pattern != null) {
			this.patternResourceMakers.add(resourceMaker);
		}
		if (columnName != null) {
			this.columnResourceMakers.add(resourceMaker);
		}
		if (!containsDuplicates(node)) {
			this.uniqueNodeMakers.add(resourceMaker);
		}
		this.resourcesConditionsMap.put(resourceMaker, findLiterals(node, D2RQ.condition));
	}

	private boolean containsDuplicates(Node node) {
		String containsDuplicates = findZeroOrOneLiteral(node, D2RQ.containsDuplicates);
		if ("true".equals(containsDuplicates)) {
			return true;
		} else if (containsDuplicates != null) {
			Logger.instance().error("Illegal value '" + containsDuplicates + "' for d2rq:containsDuplicates on " + node);
		}
		return false;
	}

	private void assertHasColumnTypes(NodeMaker nodeMaker, Database db) {
		Iterator it = nodeMaker.getColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			db.assertHasType(column);			
		}
	}

	public TranslationTable getTranslationTable(Node node) {
		if (this.translationTables.containsKey(node)) {
			return (TranslationTable) this.translationTables.get(node);
		}
		TranslationTable translationTable = new TranslationTable();
		String href = findZeroOrOneLiteralOrURI(node, D2RQ.href);
		if (href != null) {
			translationTable.addAll(new CSVParser(href).parse());
		}
		String className = findZeroOrOneLiteral(node, D2RQ.javaClass);
		if (className != null) {
			translationTable.setTranslatorClass(className, getResourceFromNode(node));
		}
		ExtendedIterator it = this.graph.find(node, D2RQ.translation, Node.ANY);
		if (href == null && className == null && !it.hasNext()) {
			Logger.instance().warning("TranslationTable " + node + " contains no translations");
		}
		if (className != null && (href != null || it.hasNext())) {
			Logger.instance().warning("Can't combine d2rq:javaClass with d2rq:translation or d2rq:href on " + node);
		}
		while (it.hasNext()) {
			Node translation = ((Triple) it.next()).getObject();
			String dbValue = findOneLiteral(translation, D2RQ.databaseValue);
			String rdfValue = findOneLiteralOrURI(translation, D2RQ.rdfValue);
			translationTable.addTranslation(dbValue, rdfValue);
		}
		this.translationTables.put(node, translationTable);
		return translationTable;
	}

	private void parsePropertyBridges() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.belongsToClassMap, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			NodeMaker resourceMaker = (NodeMaker) this.classMaps.get(t.getObject());
			if (resourceMaker == null) {
				Logger.instance().error("d2rq:belongsToClassMap for " +
						t.getSubject() + " is no d2rq:ClassMap");
				return;
			}
			buildPropertyBridge(t.getSubject(), resourceMaker);
		}
		it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.DatatypePropertyBridge);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			if (!this.propertyBridges.containsKey(t.getSubject())) {
				Logger.instance().warning("PropertyBridge " + t.getSubject() + " has no d2rq:belongsToClassMap");
			}
		}
		it = this.graph.find(Node.ANY, RDF.Nodes.type, D2RQ.ObjectPropertyBridge);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			if (!this.propertyBridges.containsKey(t.getSubject())) {
				Logger.instance().warning("PropertyBridge " + t.getSubject() + " has no d2rq:belongsToClassMap");
			}
		}
	}

	private void buildPropertyBridge(Node node, NodeMaker resourceMaker) {
		if (this.propertyBridges.containsKey(node)) {
			Logger.instance().error("Multiple d2rq:belongsToClassMap in " + node);
			return;
		}
		Node property = findPropertyForBridge(node);
		ValueSource valueSource = null;
		String columnName = findZeroOrOneLiteral(node, D2RQ.column);
		if (columnName != null) {
			valueSource = new Column(columnName);
		}
		String pattern = findZeroOrOneLiteral(node, D2RQ.pattern);
		if (pattern != null) {
			if (valueSource != null) {
				Logger.instance().error("Cannot combine d2rq:column and d2rq:pattern on PropertyBridge " + node);
				return;
			}
			valueSource = new Pattern(pattern);
		}
		String valueRegex = findZeroOrOneLiteral(node, D2RQ.valueRegex);
		if (valueRegex != null) {
			valueSource = new RegexRestriction(valueSource, valueRegex);
		}
		String valueContains = findZeroOrOneLiteral(node, D2RQ.valueContains);
		if (valueContains != null) {
			valueSource = new ContainsRestriction(valueSource, valueContains);
		}
		String valueMaxLength = findZeroOrOneLiteral(node, D2RQ.valueMaxLength);
		if (valueMaxLength != null) {
			try {
				int maxLength = Integer.parseInt(valueMaxLength);
				valueSource = new MaxLengthRestriction(valueSource, maxLength);
			} catch (NumberFormatException nfex) {
				Logger.instance().warning("Ignoring d2rq:valueMaxLength \"" +
						valueMaxLength + "\" on " + node + " (must be an integer)");
			}
		}
		Node translateWith = findZeroOrOneNode(node, D2RQ.translateWith);
		if (translateWith != null) {
			TranslationTable table = getTranslationTable(translateWith);
			if (table == null) {
				Logger.instance().error("Unknown d2rq:translateWith in " + node);
			}
			valueSource = table.getTranslatingValueSource(valueSource);
		}
		NodeMaker objectMaker;
		if (this.graph.contains(node, RDF.Nodes.type, D2RQ.DatatypePropertyBridge)) {
			if (valueSource == null) {
				Logger.instance().error("PropertyBridge " + node + " needs a d2rq:column or d2rq:pattern");
				return;
			}
			String dataType = findZeroOrOneLiteralOrURI(node, D2RQ.datatype);
			String lang = findZeroOrOneLiteral(node, D2RQ.lang);
			RDFDatatype rdfDatatype = (dataType == null) ?
					null : TypeMapper.getInstance().getSafeTypeByName(dataType);
			objectMaker = new LiteralMaker(node.toString(), valueSource, rdfDatatype, lang);
		} else if (this.graph.contains(node, RDF.Nodes.type, D2RQ.ObjectPropertyBridge)) {
			Node refersTo = findZeroOrOneNode(node, D2RQ.refersToClassMap);
			if (refersTo == null) {
				if (valueSource == null) {
					Logger.instance().error("PropertyBridge " + node + " needs a d2rq:column or d2rq:pattern");
					return;
				}
				objectMaker = new UriMaker(node.toString(), valueSource);
				if (pattern != null) {
					this.patternResourceMakers.add(objectMaker);
				}
				if (columnName != null) {
					this.columnResourceMakers.add(objectMaker);
				}
			} else {
				objectMaker = (NodeMaker) this.classMaps.get(refersTo);
				if (objectMaker == null) {
					Logger.instance().error("d2rq:refersToClassMap of " + node + " is no valid d2rq:ClassMap");
					return;
				}
				if (getDatabase(objectMaker) != getDatabase(resourceMaker)) {
					Logger.instance().error("d2rq:dataStorages for " + node + " don't match");
					return;
				}
			}
		} else {
			Logger.instance().error(node +
					" must be either d2rq:DatatypePropertyBridge or d2rq:ObjectPropertyBridge");
			return;
		}
		Map aliasMap=Alias.buildAliases(findLiterals(node,D2RQ.alias));
		Set joins=Join.buildJoins(findLiterals(node, D2RQ.join));
		PropertyBridge bridge = createPropertyBridge(node,
				resourceMaker, new FixedNodeMaker(property), objectMaker,
				joins, 
				aliasMap);
		bridge.addConditions(findLiterals(node, D2RQ.condition));
		assertHasColumnTypes(objectMaker, getDatabase(resourceMaker));
	}

	private PropertyBridge createPropertyBridge(Node node, 
	        NodeMaker subjects, NodeMaker predicates, NodeMaker objects, 
	        Set joins, Map aliasses) {
		PropertyBridge bridge = new PropertyBridge(node,
				subjects, predicates, objects,
				getDatabase(subjects), joins, aliasses);
		// TODO: duplicates handling should be factored out (maybe into seperate class for all relational algebra stuff?)
		boolean sUnique = this.uniqueNodeMakers.contains(subjects);
		boolean pUnique = this.uniqueNodeMakers.contains(predicates);
		boolean oUnique = this.uniqueNodeMakers.contains(objects);
		boolean oneOrMoreUnique = sUnique || pUnique || oUnique;
		boolean allUnique = sUnique && pUnique && oUnique;
		boolean containsNoDuplicates = (allUnique && joins.size() <= 1) ||
				(oneOrMoreUnique && joins.isEmpty());
		bridge.setMightContainDuplicates(!containsNoDuplicates);
		bridge.addConditions((Set) this.resourcesConditionsMap.get(subjects));
		bridge.addConditions((Set) this.resourcesConditionsMap.get(predicates));
		bridge.addConditions((Set) this.resourcesConditionsMap.get(objects));
		if (!joins.isEmpty()) {
			inferColumnTypesFromJoins(joins, subjects);
		}
		URIMatchPolicy policy = new URIMatchPolicy();
		if (this.columnResourceMakers.contains(subjects)) {
			policy.setSubjectBasedOnURIColumn(true);
		}
		if (this.patternResourceMakers.contains(subjects)) {
			policy.setSubjectBasedOnURIPattern(true);
		}
		if (this.columnResourceMakers.contains(objects)) {
			policy.setObjectBasedOnURIColumn(true);
		}
		if (this.patternResourceMakers.contains(objects)) {
			policy.setObjectBasedOnURIPattern(true);
		}
		bridge.setURIMatchPolicy(policy);
		this.propertyBridges.put(node, bridge);
		return bridge;
	}

	private void inferColumnTypesFromJoins(Collection joins, NodeMaker resourceMaker) {
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			getDatabase(resourceMaker).inferColumnTypes(join);
		}
	}

	private Database getDatabase(NodeMaker resourceMaker) {
		return (Database) this.resourcesDatabasesMap.get(resourceMaker);
	}

	private Node findPropertyForBridge(Node bridge) {
		Node result = null;
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.propertyBridge, bridge);
		if (it.hasNext()) {
			result = ((Triple) it.next()).getSubject();
			if (it.hasNext()) {
				Logger.instance().warning("Ignoring multiple d2rq:propertyBridges for d2rq:PropertyBridge " + bridge);
				return null;
			}
		}
		it = this.graph.find(bridge, D2RQ.property, Node.ANY);
		if (result == null && !it.hasNext()) {
			Logger.instance().error("Missing d2rq:property for PropertyBridge " + bridge);
			return null;
		}
		if (result != null && it.hasNext()) {
			Logger.instance().warning("Ignoring redundant d2rq:property for d2rq:PropertyBridge " + bridge);
			return null;
		}
		if (result != null) {
			return result;
		}
		result = ((Triple) it.next()).getObject();
		if (it.hasNext()) {
			Logger.instance().warning("Ignoring multiple d2rq:property statements for d2rq:PropertyBridge " + bridge);
			return null;
		}
		return result;
	}

	private void parseAdditionalProperties() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.additionalProperty, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			NodeMaker classMap = (NodeMaker) this.classMaps.get(t.getSubject());
			if (classMap == null) {
				Logger.instance().warning("Ignoring d2rq:additionalProperty on " +
						t.getSubject() + " as they are allowed only on d2rq:ClassMaps");
				continue;
			}
			createPropertyBridge(
					t.getObject(),
					classMap,
					new FixedNodeMaker(findOneNode(t.getObject(), D2RQ.propertyName)),
					new FixedNodeMaker(findOneNode(t.getObject(), D2RQ.propertyValue)),
					new HashSet(0), new HashMap(0));
		}
	}

	private void addRDFTypePropertyBridges() {
		ExtendedIterator it = this.graph.find(Node.ANY, D2RQ.classMap, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			addRDFTypePropertyBridge(t.getObject(), t.getSubject());
		}
		it = this.graph.find(Node.ANY, D2RQ.class_, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			addRDFTypePropertyBridge(t.getSubject(), t.getObject());
		}
	}

	private void addRDFTypePropertyBridge(Node toClassMap, Node rdfsClass) {
		NodeMaker classMap = (NodeMaker) this.classMaps.get(toClassMap);
		if (classMap == null) {
			Logger.instance().error(classMap + ", referenced from " +
					rdfsClass + ", is no d2rq:ClassMap");
			return;
		}
		createPropertyBridge(Node.createAnon(), 
				classMap,
				new FixedNodeMaker(RDF.Nodes.type),
				new FixedNodeMaker(rdfsClass),
				new HashSet(0), new HashMap(0));
	}

	private Resource getResourceFromNode(Node node) {
		return this.model.getResource(node.getURI());
	}

	private String findZeroOrOneLiteral(Node subject, Node predicate) {
		Node node = findZeroOrOneNode(subject, predicate);
		if (node == null) {
			return null;
		}
		if (!node.isLiteral()) {
			Logger.instance().error(toQName(predicate) + " for " + subject + " must be literal");
			return null;
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
		Logger.instance().error(toQName(predicate) + " for " + subject + " must be literal or URI");
		return null;
	}

	private Node findZeroOrOneNode(Node subject, Node predicate) {
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		if (!it.hasNext()) {
			return null;
		}
		Node result = ((Triple) it.next()).getObject();
		if (it.hasNext()) {
			Logger.instance().warning("Ignoring multiple " + toQName(predicate) + " on " + subject);
			return null;
		}
		return result;
	}

	private Node findOneNode(Node subject, Node predicate) {
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		if (!it.hasNext()) {
			Logger.instance().error("Missing " + toQName(predicate) +
					" in d2rq:Translation " + subject);
			return null;
		}
		Node result = ((Triple) it.next()).getObject();
		if (it.hasNext()) {
			Logger.instance().warning("Ignoring multiple " + toQName(predicate) + " on " + subject);
		}
		return result;
	}

	private String findOneLiteral(Node subject, Node predicate) {
		Node node = findOneNode(subject, predicate);
		if (!node.isLiteral()) {
			Logger.instance().error(toQName(predicate) + " for " + subject + " must be literal");
			return null;
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
		Logger.instance().error(toQName(predicate) + " for " + subject + " must be literal or URI");
		return null;
	}

	private Set findLiterals(Node subject, Node predicate) {
		Set result = new HashSet(3);
		ExtendedIterator it = this.graph.find(subject, predicate, Node.ANY);
		while (it.hasNext()) {
			Node node = ((Triple) it.next()).getObject();
			if (!node.isLiteral()) {
				Logger.instance().error(toQName(predicate) + " for " + subject + " must be literal");
				return null;
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
			        Logger.instance().warning("Ignoring non-literal " + toQName(predicate) +
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
		return node.toString(this.prefixes);
	}
}