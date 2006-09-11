package de.fuberlin.wiwiss.d2rq.parser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.csv.CSVParser;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.ResourceMap;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Creates a {@link Mapping} from a Jena model representation
 * of a D2RQ mapping file.
 * 
 * TODO: Clean up Database section
 * TODO: Clean up TranslationTable section
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MapParser.java,v 1.15 2006/09/11 23:22:25 cyganiak Exp $
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
	private OntModel model;
	private Graph graph;
	private String baseURI;
	private Map nodesToTranslationTables = new HashMap();
	private Mapping mapping;
	
	/**
	 * Constructs a new MapParser from a Jena model containing the RDF statements
	 * from a D2RQ mapping file.
	 * @param mapModel a Jena model containing the RDF statements from a D2RQ mapping file
	 */
	public MapParser(Model mapModel, String baseURI) {
		this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, mapModel);
		this.graph = mapModel.getGraph();
		this.baseURI = absolutizeURI(baseURI);
	}
	
	/**
	 * Starts the parsing process. Must be called before results can be retrieved
	 * from the getter methods.
	 */
	public Mapping parse() {
		if (this.mapping != null) {
			return this.mapping;
		}
		this.mapping = new Mapping();
	    parseProcessingInstructions();
		parseDatabases();
		parseClassMaps();
		parsePropertyBridges();
		return this.mapping;
	}

	private void parseProcessingInstructions() {
		Iterator it = this.model.listIndividuals(D2RQ.ProcessingInstructions);
		while (it.hasNext()) {
			Resource instructions = (Resource) it.next();
			StmtIterator it2 = instructions.listProperties();
			while (it2.hasNext()) {
				Statement stmt = (Statement) it2.next();
				if (!stmt.getObject().isLiteral()) {
					this.mapping.setProcessingInstruction(stmt.getPredicate(), stmt.getString());
				}
			}
		}
	}
	
	private void parseDatabases() {
		ExtendedIterator it = this.model.listIndividuals(D2RQ.Database);
		while (it.hasNext()) {
			Resource database = (Resource) it.next();
			this.mapping.addDatabase(database, buildDatabase(database.asNode()));
		}
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
		Iterator it = this.model.listIndividuals(D2RQ.ClassMap);
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			ClassMap classMap = new ClassMap(r);
			parseClassMap(classMap, r);
			parseResourceMap(classMap, r);
			this.mapping.addClassMap(classMap);
		}
	}

	private void parseResourceMap(ResourceMap resourceMap, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.bNodeIdColumns);
		while (stmts.hasNext()) {
			resourceMap.setBNodeIdColumns(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.uriColumn);
		while (stmts.hasNext()) {
			resourceMap.setURIColumn(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.uriPattern);
		while (stmts.hasNext()) {
			resourceMap.setURIPattern(ensureIsAbsolute(stmts.nextStatement().getString()));
		}
		stmts = r.listProperties(D2RQ.valueRegex);
		while (stmts.hasNext()) {
			resourceMap.addValueRegex(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.valueContains);
		while (stmts.hasNext()) {
			resourceMap.addValueContains(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.valueMaxLength);
		while (stmts.hasNext()) {
			String s = stmts.nextStatement().getString();
			try {
				resourceMap.setValueMaxLength(Integer.parseInt(s));
			} catch (NumberFormatException nfex) {
				throw new D2RQException("d2rq:valueMaxLength \"" + s + "\" on " + 
						PrettyPrinter.toString(r) + " must be an integer number");
			}
		}
		stmts = r.listProperties(D2RQ.join);
		while (stmts.hasNext()) {
			resourceMap.addJoin(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.condition);
		while (stmts.hasNext()) {
			resourceMap.addCondition(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.alias);
		while (stmts.hasNext()) {
			resourceMap.addAlias(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.containsDuplicates);
		while (stmts.hasNext()) {
			String containsDuplicates = stmts.nextStatement().getString();
			if ("true".equals(containsDuplicates)) {
				resourceMap.setContainsDuplicates(true);
			} else if ("false".equals(containsDuplicates)) {
				resourceMap.setContainsDuplicates(false);
			} else if (containsDuplicates != null) {
				throw new D2RQException("Illegal value '" + containsDuplicates + 
						"' for d2rq:containsDuplicates on " + PrettyPrinter.toString(r),
						D2RQException.RESOURCEMAP_ILLEGAL_CONTAINSDUPLICATE);
			}
		}
		stmts = r.listProperties(D2RQ.translateWith);
		while (stmts.hasNext()) {
			TranslationTable table = getTranslationTable(stmts.nextStatement().getResource());
			resourceMap.setTranslateWith(table);
		}
	}
	
	private void parseClassMap(ClassMap classMap, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.dataStorage);
		while (stmts.hasNext()) {
			classMap.setDatabase(this.mapping.database(
					stmts.nextStatement().getResource()));
		}
		stmts = r.listProperties(D2RQ.class_);
		while (stmts.hasNext()) {
			classMap.addClass(stmts.nextStatement().getResource());
		}
		stmts = this.model.listStatements(null, D2RQ.classMap, r);
		while (stmts.hasNext()) {
			classMap.addClass(stmts.nextStatement().getSubject());
		}
		stmts = r.listProperties(D2RQ.additionalProperty);
		while (stmts.hasNext()) {
			Resource additionalProperty = stmts.nextStatement().getResource();
			classMap.addAdditionalProperty(
					additionalProperty.getProperty(D2RQ.propertyName).getResource(),
					additionalProperty.getProperty(D2RQ.propertyValue).getObject());
		}
	}
	
	private void parsePropertyBridges() {
		StmtIterator stmts = this.model.listStatements(null, D2RQ.belongsToClassMap, (RDFNode) null);
		while (stmts.hasNext()) {
			Statement stmt = stmts.nextStatement();
			ClassMap classMap = this.mapping.classMap(stmt.getResource());
			Resource r = stmt.getSubject();
			PropertyBridge bridge = new PropertyBridge(r);
			bridge.setBelongsToClassMap(classMap);
			parseResourceMap(bridge, r);
			parsePropertyBridge(bridge, r);
			classMap.addPropertyBridge(bridge);
		}
	}
	
	private void parsePropertyBridge(PropertyBridge bridge, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.column);
		while (stmts.hasNext()) {
			if (r.getProperty(RDF.type).equals(D2RQ.ObjectPropertyBridge)) {
				// Legacy
				bridge.setURIColumn(stmts.nextStatement().getString());
			} else {
				bridge.setColumn(stmts.nextStatement().getString());
			}
		}
		stmts = r.listProperties(D2RQ.pattern);
		while (stmts.hasNext()) {
			if (r.getProperty(RDF.type).equals(D2RQ.ObjectPropertyBridge)) {
				// Legacy
				bridge.setURIPattern(stmts.nextStatement().getString());
			} else {
				bridge.setPattern(stmts.nextStatement().getString());
			}
		}
		stmts = r.listProperties(D2RQ.lang);
		while (stmts.hasNext()) {
			bridge.setLang(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.datatype);
		while (stmts.hasNext()) {
			bridge.setDatatype(stmts.nextStatement().getResource().getURI());
		}
		stmts = r.listProperties(D2RQ.refersToClassMap);
		while (stmts.hasNext()) {
			Resource classMapResource = stmts.nextStatement().getResource();
			bridge.setRefersToClassMap(this.mapping.classMap(classMapResource));
		}
		stmts = r.listProperties(D2RQ.property);
		while (stmts.hasNext()) {
			bridge.addProperty(stmts.nextStatement().getResource());
		}
		stmts = this.model.listStatements(null, D2RQ.propertyBridge, r);
		while (stmts.hasNext()) {
			bridge.addProperty(stmts.nextStatement().getSubject());
		}
	}

	public TranslationTable getTranslationTable(Resource r) {
		Node node = r.asNode();
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
			translationTable.setTranslatorClass(className, this.model.getResource(node.getURI()));
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
}