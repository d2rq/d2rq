package de.fuberlin.wiwiss.d2rq.parser;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.util.RelURI;
import com.hp.hpl.jena.rdf.model.LiteralRequiredException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
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
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MapParser.java,v 1.20 2006/09/28 12:15:54 cyganiak Exp $
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
		return RelURI.resolve(uri);
	}
	
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
		this.mapping = new Mapping();
		try {
		    parseProcessingInstructions();
			parseDatabases();
			parseTranslationTables();
			parseClassMaps();
			parsePropertyBridges();
		} catch (LiteralRequiredException ex) {
			throw new D2RQException("Expected URI resource, found literal instead: " + ex.getMessage(),
					D2RQException.MAPPING_RESOURCE_INSTEADOF_LITERAL);
		} catch (ResourceRequiredException ex) {
			throw new D2RQException("Expected literal, found URI resource instead: " + ex.getMessage(),
					D2RQException.MAPPING_LITERAL_INSTEADOF_RESOURCE);
		}
		return this.mapping;
	}

	private void parseProcessingInstructions() {
		Iterator it = this.model.listIndividuals(D2RQ.ProcessingInstructions);
		while (it.hasNext()) {
			Resource instructions = (Resource) it.next();
			StmtIterator it2 = instructions.listProperties();
			while (it2.hasNext()) {
				Statement stmt = (Statement) it2.next();
				if (stmt.getObject().isLiteral()) {
					this.mapping.setProcessingInstruction(stmt.getPredicate(), stmt.getString());
				}
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
		stmts = r.listProperties(D2RQ.expressionTranslator);
		while (stmts.hasNext()) {
			database.setExpressionTranslator(stmts.nextStatement().getString());
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
			String rdf = translation.getProperty(D2RQ.rdfValue).getString();
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

	// TODO: I guess this should be done at map compile time
	private String ensureIsAbsolute(String uriPattern) {
		if (uriPattern.indexOf(":") == -1) {
			return this.baseURI + uriPattern;
		}
		return uriPattern;
	}
}