package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.n3.IRIResolver;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.LiteralRequiredException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.mapgen.Filter;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.vocab.RR;
import de.fuberlin.wiwiss.d2rq.vocab.VocabularySummarizer;

/**
 * Creates a {@link Mapping} from a Jena model representation
 * of a R2RML mapping file.
 * 
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class R2RMLMapParser {
	private final static Log log = LogFactory.getLog(R2RMLMapParser.class);
	
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
	private Database database;
	protected final ConnectedDB connectedDB;

	private String schemaName;
	
	/**
	 * Constructs a new R2RMLMapParser from a Jena model containing the RDF statements
	 * from a R2RML mapping file.
	 * @param mapModel a Jena model containing the RDF statements from a R2RML mapping file
	 * @param baseURI used for relative URI patterns
	 * @param database 
	 * @param filter 
	 */
	public R2RMLMapParser(Model mapModel, String baseURI, Database database, Filter filter) {
		this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, mapModel);
		this.baseURI = absolutizeURI(baseURI);
		this.database = database;
		this.connectedDB = new ConnectedDB(database.getJdbcURL(), database.getUsername(), database.getPassword());
		this.schemaName = filter != null ? filter.getSingleSchema() : "";
	}
	
	/**
	 * Starts the parsing process. Must be called before results can be retrieved
	 * from the getter methods.
	 */
	public Mapping parse() {
		if (this.mapping != null) {
			return this.mapping;
		}
		findUnknownRRTerms();
		ensureAllDistinct(new Resource[]{RR.TriplesMap, RR.logicalTable, 
				RR.template}, D2RQException.MAPPING_TYPECONFLICT);
		this.mapping = new Mapping();
		copyPrefixes();
		try {
			this.mapping.addDatabase(database);
			parseTriplesMaps();
			this.mapping.buildVocabularyModel();
			log.info("Done reading R2RML map with " + 
					mapping.databases().size() + " databases and " +
					mapping.classMapResources().size() + " class maps");
			return this.mapping;
		} catch (LiteralRequiredException ex) {
			throw new D2RQException("Expected literal, found URI resource instead: " + ex.getMessage(),
					D2RQException.MAPPING_RESOURCE_INSTEADOF_LITERAL);
		} catch (ResourceRequiredException ex) {
			throw new D2RQException("Expected URI, found literal instead: " + ex.getMessage(),
					D2RQException.MAPPING_LITERAL_INSTEADOF_RESOURCE);
		}
	}
	
	private void ensureAllDistinct(Resource[] distinctClasses, int errorCode) {
		Collection<Resource> classes = Arrays.asList(distinctClasses);
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

	private void findUnknownRRTerms() {
		VocabularySummarizer rrTerms = new VocabularySummarizer(RR.class);
		StmtIterator it = this.model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().getURI().startsWith(RR.NS) 
					&& !rrTerms.getAllProperties().contains(stmt.getPredicate())) {
				throw new D2RQException(
						"Unknown property " + PrettyPrinter.toString(stmt.getPredicate()) + ", maybe a typo?",
						D2RQException.MAPPING_UNKNOWN_D2RQ_PROPERTY);
			}
			if (stmt.getPredicate().equals(RDF.type)
					&& stmt.getObject().isURIResource()
					&& stmt.getResource().getURI().startsWith(RR.NS)
					&& !rrTerms.getAllClasses().contains(stmt.getObject())) {
				throw new D2RQException(
						"Unknown class r2rml:" + PrettyPrinter.toString(stmt.getObject()) + ", maybe a typo?",
						D2RQException.MAPPING_UNKNOWN_D2RQ_CLASS);
			}
		}
	}

	private void parseTriplesMaps() {
		ResIterator it = this.model.listResourcesWithProperty(RR.logicalTable);
		while (it.hasNext()) {
			Resource r = it.next();
			ClassMap classMap = new ClassMap(r);
			parseTripleMap(classMap, r);
			this.mapping.addClassMap(classMap);
		}
	}

	private void parseTripleMap(ClassMap classMap, Resource tripleMap) {
		Statement stmt;
		classMap.setDatabase(this.database);
		
		stmt = tripleMap.getProperty(RR.logicalTable);
		Resource logicalTable = stmt.getResource();
		stmt = logicalTable.getProperty(RR.tableName);
		classMap.setLogicalTable(stmt.getString());
		
		stmt = tripleMap.getProperty(RR.subjectMap);
		Resource subjectMap = stmt.getResource();
		stmt = subjectMap.getProperty(RR.class_);
		Resource class_ = stmt.getResource();
		classMap.addClass(class_);
		stmt = subjectMap.getProperty(RR.template);
		String uriTemplate = genUriTemplate(classMap.getLogicalTable(), stmt.getString());
		classMap.setURIPattern(ensureIsAbsolute(uriTemplate));
		
		parsePredicateObjectMaps(classMap, tripleMap);
	}
	
	private String genUriTemplate(String logicalTable, String originalUri) {
		String[] uriParts = originalUri.split("[{]");
		String column = uriParts[1];
		column = column.substring(0, column.length() - 1);
		
		StringBuffer template = new StringBuffer(uriParts[0]);
		template.append("@@");
		template.append(logicalTable);
		template.append(".");
		template.append(column);
		
		Attribute attribute = new Attribute(new RelationName(schemaName, logicalTable), column); 
		if (!connectedDB.columnType(attribute).isIRISafe()) {
			template.append("|encode");
		}
		template.append("@@");
		
		return template.toString();
	}

	private void parsePredicateObjectMaps(ClassMap classMap, Resource tripleMap) {
		StmtIterator stmts;
		stmts = tripleMap.listProperties(RR.predicateObjectMap);
		while (stmts.hasNext()) {
			Resource pObjMap = stmts.nextStatement().getResource();
			
			PropertyBridge bridge = new PropertyBridge(pObjMap);
			bridge.setBelongsToClassMap(classMap);
			parsePredicateObjectMap(classMap, bridge, pObjMap);
			classMap.addPropertyBridge(bridge);
		}
	}
	
	private void parsePredicateObjectMap(ClassMap classMap, PropertyBridge bridge, Resource predObjMap) {
		Statement stmt;
		stmt = predObjMap.getProperty(RR.predicate);
		bridge.addProperty(stmt.getResource());
		
		stmt = predObjMap.getProperty(RR.objectMap);
		Resource objectMap = stmt.getResource();
		stmt = objectMap.getProperty(RR.column);
		if (stmt != null) {
			String column = stmt.getString();
			bridge.setColumn(classMap.getLogicalTable() + "." + column);
		} else {
			stmt = objectMap.getProperty(RR.parentTriplesMap);

			ClassMap classMapRef = this.mapping.classMap(stmt.getResource());
			bridge.setRefersToClassMap(classMapRef);
			
			stmt = objectMap.getProperty(RR.joinCondition);
			Resource joinCondition = stmt.getResource();
			String child = joinCondition.getProperty(RR.child).getString();
			String childLogTable = classMap.getLogicalTable();
			String parent = joinCondition.getProperty(RR.parent).getString();
			String parentLogTable = classMapRef.getLogicalTable();
			
			bridge.addJoin(childLogTable + "." + child + " => " + parentLogTable + "." + parent);
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