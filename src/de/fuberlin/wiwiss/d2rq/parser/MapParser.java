package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.hp.hpl.jena.n3.IRIResolver;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.LiteralRequiredException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Configuration;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.DynamicProperty;
import de.fuberlin.wiwiss.d2rq.map.ResourceMap;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.JDBC;
import de.fuberlin.wiwiss.d2rq.vocab.VocabularySummarizer;

/**
 * Creates a {@link Mapping} from a Jena model representation
 * of a D2RQ mapping file.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MapParser.java,v 1.41 2009/07/31 21:13:44 fatorange Exp $
 */
public class MapParser {

	/**
	 * A regular expression that matches zero or more characters that are allowed inside URIs
	 */
	public static final String URI_CHAR_REGEX = "([:/?#\\[\\]@!$&'()*+,;=a-zA-Z0-9._~-]|%[0-9A-Fa-f][0-9A-Fa-f])*";	

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
		return resolver.resolve(uri);
	}
	private final static IRIResolver resolver = new IRIResolver();

	private OntModel model;
	private String baseURI;
	private Mapping mapping;
	
	/**
	 * Constructs a new MapParser from a Jena model containing the RDF statements
	 * from a D2RQ mapping file.
	 * @param mapModel a Jena model containing the RDF statements from a D2RQ mapping file
	 */
	public MapParser(Model mapModel, String baseURI) {
		this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, mapModel);
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
		findUnknownD2RQTerms();
		ensureAllDistinct(new Resource[]{D2RQ.Database, D2RQ.ClassMap, D2RQ.PropertyBridge, 
				D2RQ.TranslationTable, D2RQ.Translation}, D2RQException.MAPPING_TYPECONFLICT);
		this.mapping = new Mapping();
		copyPrefixes();
		try {
			parseDatabases();
			parseConfiguration();
			parseTranslationTables();
			parseClassMaps();
			parsePropertyBridges();
			this.mapping.buildVocabularyModel();
		} catch (LiteralRequiredException ex) {
			throw new D2RQException("Expected URI resource, found literal instead: " + ex.getMessage(),
					D2RQException.MAPPING_RESOURCE_INSTEADOF_LITERAL);
		} catch (ResourceRequiredException ex) {
			throw new D2RQException("Expected literal, found URI resource instead: " + ex.getMessage(),
					D2RQException.MAPPING_LITERAL_INSTEADOF_RESOURCE);
		}
		return this.mapping;
	}
	
	private void ensureAllDistinct(Resource[] distinctClasses, int errorCode) {
		Collection classes = Arrays.asList(distinctClasses);
		ResIterator it = this.model.listSubjects();
		while (it.hasNext()) {
			Resource resource = it.nextResource();
			Resource matchingType = null;
			StmtIterator typeIt = resource.listProperties(RDF.type);
			while (typeIt.hasNext()) {
				Resource type = typeIt.nextStatement().getResource();
				if (!classes.contains(type)) continue;
				if (matchingType == null) {
					matchingType = type;
				} else {
					throw new D2RQException("Name " + PrettyPrinter.toString(resource) + " cannot be both a "
							+ PrettyPrinter.toString(matchingType) + " and a " + PrettyPrinter.toString(type),
							errorCode);
				}
			}
		}
	}
	
	/**
	 * Copies all prefixes from the mapping file Model to the D2RQ mapping.
	 * Administrative D2RQ prefixes are dropped on the assumption that they
	 * are not wanted in the actual data.
	 */ 
	private void copyPrefixes() {
		mapping.getPrefixMapping().setNsPrefixes(model);
		Iterator it = mapping.getPrefixMapping().getNsPrefixMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String namespace = (String) entry.getValue();
			if (D2RQ.NS.equals(namespace) && "d2rq".equals(entry.getKey())) {
				mapping.getPrefixMapping().removeNsPrefix((String) entry.getKey());
			}
			if (JDBC.NS.equals(namespace) && "jdbc".equals(entry.getKey())) {
				mapping.getPrefixMapping().removeNsPrefix((String) entry.getKey());
			}
		}
	}

	private void findUnknownD2RQTerms() {
		VocabularySummarizer d2rqTerms = new VocabularySummarizer(D2RQ.class);
		StmtIterator it = this.model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().getURI().startsWith(D2RQ.NS) 
					&& !d2rqTerms.getAllProperties().contains(stmt.getPredicate())) {
				throw new D2RQException(
						"Unknown property " + PrettyPrinter.toString(stmt.getPredicate()) + ", maybe a typo?",
						D2RQException.MAPPING_UNKNOWN_D2RQ_PROPERTY);
			}
			if (stmt.getPredicate().equals(RDF.type)
					&& stmt.getObject().isURIResource()
					&& stmt.getResource().getURI().startsWith(D2RQ.NS)
					&& !d2rqTerms.getAllClasses().contains(stmt.getObject())) {
				throw new D2RQException(
						"Unknown class d2rq:" + PrettyPrinter.toString(stmt.getObject()) + ", maybe a typo?",
						D2RQException.MAPPING_UNKNOWN_D2RQ_CLASS);
			}
		}
	}
	
	private void parseDatabases() {
		Iterator it = this.model.listIndividuals(D2RQ.Database);
		while (it.hasNext()) {
			Resource dbResource = (Resource) it.next();
			Database database = new Database(dbResource);
			parseDatabase(database, dbResource);
			this.mapping.addDatabase(database);
		}
	}
	
	private void parseConfiguration() {
		Iterator it = this.model.listIndividuals(D2RQ.Configuration);
		if (it.hasNext()) {
			Resource configResource = (Resource) it.next();
			Configuration configuration = new Configuration(configResource);
			StmtIterator stmts = configResource.listProperties(D2RQ.serveVocabulary);
			while (stmts.hasNext()) {
				configuration.setServeVocabulary(stmts.nextStatement().getBoolean());
			}			
			this.mapping.setConfiguration(configuration);

			if (it.hasNext())
				throw new D2RQException("Only one configuration block is allowed");
		}
	}

	private void parseDatabase(Database database, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.odbcDSN);
		while (stmts.hasNext()) {
			database.setODBCDSN(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.jdbcDSN);
		while (stmts.hasNext()) {
			database.setJDBCDSN(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.jdbcDriver);
		while (stmts.hasNext()) {
			database.setJDBCDriver(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.username);
		while (stmts.hasNext()) {
			database.setUsername(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.password);
		while (stmts.hasNext()) {
			database.setPassword(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.allowDistinct);
		while (stmts.hasNext()) {
			String allowDistinct = stmts.nextStatement().getString();
			if (allowDistinct.equals("true")) {
				database.setAllowDistinct(true);
			} else if (allowDistinct.equals("false")) {
				database.setAllowDistinct(false);
			} else {
				throw new D2RQException("d2rq:allowDistinct value must be true or false");
			}
		}
		stmts = r.listProperties(D2RQ.resultSizeLimit);
		while (stmts.hasNext()) {
			try {
				int limit = Integer.parseInt(stmts.nextStatement().getString());
				database.setResultSizeLimit(limit);
			} catch (NumberFormatException ex) {
				throw new D2RQException("Value of d2rq:resultSizeLimit must be numeric", D2RQException.MUST_BE_NUMERIC);
			}
		}
		stmts = r.listProperties(D2RQ.textColumn);
		while (stmts.hasNext()) {
			database.addTextColumn(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.numericColumn);
		while (stmts.hasNext()) {
			database.addNumericColumn(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.dateColumn);
		while (stmts.hasNext()) {
			database.addDateColumn(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.timestampColumn);
		while (stmts.hasNext()) {
			database.addTimestampColumn(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.fetchSize);
		while (stmts.hasNext()) {
			try {
				int fetchSize = Integer.parseInt(stmts.nextStatement().getString());
				database.setFetchSize(fetchSize);
			} catch (NumberFormatException ex) {
				throw new D2RQException("Value of d2rq:fetchSize must be numeric", D2RQException.MUST_BE_NUMERIC);
			}
		}
		stmts = r.listProperties();
		while (stmts.hasNext()) {
			Statement stmt = stmts.nextStatement();
			String prop = stmt.getPredicate().getURI();
			if (!prop.startsWith(JDBC.NS)) continue;
			database.setConnectionProperty(
					prop.substring(JDBC.NS.length()), stmt.getString());
		}
	}

	private void parseTranslationTables() {
		Set translationTableResources = new HashSet();
		Iterator it = this.model.listIndividuals(D2RQ.TranslationTable);
		while (it.hasNext()) {
			translationTableResources.add(it.next());
		}
		StmtIterator stmts;
		stmts = this.model.listStatements(null, D2RQ.translateWith, (Resource) null);
		while (stmts.hasNext()) {
			translationTableResources.add(stmts.nextStatement().getResource());
		}
		stmts = this.model.listStatements(null, D2RQ.translation, (RDFNode) null);
		while (stmts.hasNext()) {
			translationTableResources.add(stmts.nextStatement().getSubject());
		}
		stmts = this.model.listStatements(null, D2RQ.javaClass, (RDFNode) null);
		while (stmts.hasNext()) {
			translationTableResources.add(stmts.nextStatement().getSubject());
		}
		stmts = this.model.listStatements(null, D2RQ.href, (RDFNode) null);
		while (stmts.hasNext()) {
			translationTableResources.add(stmts.nextStatement().getSubject());
		}
		it = translationTableResources.iterator();
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			TranslationTable table = new TranslationTable(r);
			parseTranslationTable(table, r);
			this.mapping.addTranslationTable(table);
		}
	}

	private void parseTranslationTable(TranslationTable table, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.href);
		while (stmts.hasNext()) {
			table.setHref(stmts.nextStatement().getResource().getURI());
		}
		stmts = r.listProperties(D2RQ.javaClass);
		while (stmts.hasNext()) {
			table.setJavaClass(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.translation);
		while (stmts.hasNext()) {
			Resource translation = stmts.nextStatement().getResource();
			String db = translation.getProperty(D2RQ.databaseValue).getString();
			Statement stmt = translation.getProperty(D2RQ.rdfValue);
			String rdf = stmt.getObject().isLiteral() ? stmt.getString() : stmt.getResource().getURI();
			table.addTranslation(db, rdf);
		}
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
		stmts = r.listProperties(D2RQ.constantValue);
		while (stmts.hasNext()) {
			resourceMap.setConstantValue(stmts.nextStatement().getObject());
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
			resourceMap.setTranslateWith(this.mapping.translationTable(stmts.nextStatement().getResource()));
		}
	}
	
	private void parseClassMap(ClassMap classMap, Resource r) {
		StmtIterator stmts;
		stmts = r.listProperties(D2RQ.dataStorage);
		while (stmts.hasNext()) {
			classMap.setDatabase(this.mapping.database(
					stmts.nextStatement().getResource()));
		}
		stmts = r.listProperties(D2RQ.class__);
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
			PropertyBridge bridge = new PropertyBridge(r);
			bridge.setBelongsToClassMap(classMap);
			bridge.addProperty(additionalProperty.getProperty(D2RQ.propertyName).getResource());
			bridge.setConstantValue(additionalProperty.getProperty(D2RQ.propertyValue).getObject());
			classMap.addPropertyBridge(bridge);
		}
		stmts = r.listProperties(D2RQ.class_DefinitionLabel);
		while (stmts.hasNext()) {
			classMap.addDefinitionLabel(stmts.nextStatement().getLiteral());
		}
		stmts = r.listProperties(D2RQ.class_DefinitionComment);
		while (stmts.hasNext()) {
			classMap.addDefinitionComment(stmts.nextStatement().getLiteral());
		}
		stmts = r.listProperties(D2RQ.additionalClassDefinitionProperty);
		while (stmts.hasNext()) {
			Resource additionalProperty = stmts.nextStatement().getResource();
			classMap.addDefinitionProperty(additionalProperty);
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
		stmts = r.listProperties(D2RQ.sqlExpression);
		while (stmts.hasNext()) {
			bridge.setSQLExpression(stmts.nextStatement().getString());
		}
		stmts = r.listProperties(D2RQ.uriSqlExpression);
		while (stmts.hasNext()) {
			bridge.setUriSQLExpression(stmts.nextStatement().getString());
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
		stmts = r.listProperties(D2RQ.dynamicProperty);
		while (stmts.hasNext()) {
			String dynamicPropertyPattern = stmts.nextStatement().getString();
			DynamicProperty dynamicProperty = new DynamicProperty(dynamicPropertyPattern);
			bridge.addProperty(dynamicProperty);
			this.mapping.setHasDynamicProperties(true);
		}
		stmts = r.listProperties(D2RQ.property);
		while (stmts.hasNext()) {
			bridge.addProperty(stmts.nextStatement().getResource());
		}
		stmts = this.model.listStatements(null, D2RQ.propertyBridge, r);
		while (stmts.hasNext()) {
			bridge.addProperty(stmts.nextStatement().getSubject());
		}
		stmts = r.listProperties(D2RQ.propertyDefinitionLabel);
		while (stmts.hasNext()) {
			bridge.addDefinitionLabel(stmts.nextStatement().getLiteral());
		}
		stmts = r.listProperties(D2RQ.propertyDefinitionComment);
		while (stmts.hasNext()) {
			bridge.addDefinitionComment(stmts.nextStatement().getLiteral());
		}
		stmts = r.listProperties(D2RQ.additionalPropertyDefinitionProperty);
		while (stmts.hasNext()) {
			Resource additionalProperty = stmts.nextStatement().getResource();
			bridge.addDefinitionProperty(additionalProperty);
		}
		stmts = r.listProperties(D2RQ.limit);
		while (stmts.hasNext()) {
		    bridge.setLimit(stmts.nextStatement().getInt());
		}
		stmts = r.listProperties(D2RQ.limitInverse);
		while (stmts.hasNext()) {
		    bridge.setLimitInverse(stmts.nextStatement().getInt());
		}
		stmts = r.listProperties(D2RQ.orderDesc);
		while (stmts.hasNext()) {
		    bridge.setOrder(stmts.nextStatement().getString(), true);
		}
		stmts = r.listProperties(D2RQ.orderAsc);
		while (stmts.hasNext()) {
		    bridge.setOrder(stmts.nextStatement().getString(), false);
		}
	}

	// TODO: I guess this should be done at map compile time
	private String ensureIsAbsolute(String uriPattern) {
		if (uriPattern.indexOf(":") == -1) {
			return this.baseURI + uriPattern;
		}
		return uriPattern;
	}
}