package org.d2rq.mapgen;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.lang.AliasDeclaration;
import org.d2rq.lang.ClassMap;
import org.d2rq.lang.Configuration;
import org.d2rq.lang.D2RQWriter;
import org.d2rq.lang.Database;
import org.d2rq.lang.Join;
import org.d2rq.lang.Mapping;
import org.d2rq.lang.Microsyntax;
import org.d2rq.lang.PropertyBridge;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping.IllegalPrefixException;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;


/**
 * Generates a D2RQ mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MappingGenerator {
	private final static Logger log = Logger.getLogger(MappingGenerator.class);
	private final static String CREATOR = "D2RQ Mapping Generator";

	private final MappingStyle style;
	private final SQLConnection sqlConnection;
	private final Model model;
	private final UniqueLocalNameGenerator stringMaker = new UniqueLocalNameGenerator();
	private Mapping mapping = null;
	private String mapNamespaceURI = "#";;
	private Filter filter = Filter.ALL;
	private Model vocabModel = ModelFactory.createDefaultModel();
	private boolean finished = false;
	private boolean generateClasses = true;
	private boolean generateLabelBridges = true;
	private boolean generateDefinitionLabels = true;
	private boolean handleLinkTables = true;
	private boolean serveVocabulary = true;
	private boolean skipForeignKeyTargetColumns = true;
	private boolean useUniqueKeysAsEntityID = true;
	private boolean suppressWarnings = false;
	private URI startupSQLScript;
	private final List<TableName> tablesWithoutUniqueKey = 
			new ArrayList<TableName>();
	private final Map<PropertyBridge,TableName> refersToClassMaps = 
			new HashMap<PropertyBridge,TableName>();
	private final Map<PropertyBridge,TableName> belongsToClassMaps = 
			new HashMap<PropertyBridge,TableName>();
	
	public MappingGenerator(MappingStyle style, SQLConnection sqlConnection,
			Model model) {
		this.style = style;
		this.sqlConnection = sqlConnection;
		this.model = model;
	}

	public void setMapNamespaceURI(String uri) {
		this.mapNamespaceURI = uri;
	}
	
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	public void setStartupSQLScript(URI uri) {
		startupSQLScript = uri;
	}
	
	/**
	 * @param flag Generate an rdfs:label property bridge based on the PK?
	 */
	public void setGenerateLabelBridges(boolean flag) {
		this.generateLabelBridges = flag;
	}

	/**
	 * @param flag Generate a d2rq:class for every class map?
	 */
	public void setGenerateClasses(boolean flag) {
		this.generateClasses = flag;
	}
		

	/**
	 * @param flag Handle Link Tables as properties (true) or normal tables (false)
	 */
	public void setHandleLinkTables(boolean flag) {
		this.handleLinkTables = flag;
	}

	/**
	 * @param flag Generate ClassDefinitionLabels and PropertyDefinitionLabels?
	 */
	public void setGenerateDefinitionLabels(boolean flag) {
		this.generateDefinitionLabels = flag;
	}

	/**
	 * @param flag Value for d2rq:serveVocabulary in map:Configuration
	 */
	public void setServeVocabulary(boolean flag) {
		this.serveVocabulary = flag;
	}

	public void setSkipForeignKeyTargetColumns(boolean flag) {
		skipForeignKeyTargetColumns = flag;
	}
	
	public void setUseUniqueKeysAsEntityID(boolean flag) {
		useUniqueKeysAsEntityID = flag;
	}
	
	public void setSuppressWarnings(boolean flag) {
		suppressWarnings = flag;
	}
	
	public Model getVocabularyModel() {
		writeMapping();
		return this.vocabModel;
	}
	
	/**
	 * Returns an in-memory Jena model containing the D2RQ mapping.
	 * 
	 * @param baseURI Base URI for resolving relative URIs in the mapping, e.g., map namespace
	 * @return In-memory Jena model containing the D2RQ mapping
	 */
	public Model getMappingModel(String baseURI) {
		writeMapping();
		StringWriter out = new StringWriter();
		new D2RQWriter(mapping).write(out);
		String mappingAsTurtle = out.toString();
		Model result = ModelFactory.createDefaultModel();
		result.read(new StringReader(mappingAsTurtle), baseURI, "TURTLE");
		return result;
	}
	
	/**
	 * Writes the D2RQ mapping to a writer. Note that the calling code is
	 * responsible for closing the writers after use.
	 * 
	 * @param out Stream for writing data to the file or console
	 */
	public Mapping getMapping() {
		writeMapping();
		return mapping;
	}
	
	private void writeMapping() {
		if (finished) return;
		
		initVocabularyModel();

		model.setNsPrefix("map", mapNamespaceURI);
		mapping = new Mapping();
		for (String prefix: mapping.getPrefixes().getNsPrefixMap().keySet()) {
			model.setNsPrefix(prefix, mapping.getPrefixes().getNsPrefixURI(prefix));
		}
		mapping.getPrefixes().setNsPrefixes(model);

		log.info("Generating d2rq:Configuration instance");
		mapping.setConfiguration(getConfiguration());

		log.info("Generating d2rq:Database instance");
		Database database = getDatabase();
		mapping.addDatabase(database);

		List<TableName> tableNames = new ArrayList<TableName>();
		for (TableName tableName: sqlConnection.getTableNames(filter.getSingleSchema())) {
			if (!filter.matches(tableName)) {
				log.info("Skipping table " + tableName);
				continue;
			}
			tableNames.add(tableName);
		}
		log.info("Filter '" + filter + "' matches " + tableNames.size() + " total tables");
		for (TableName tableName: tableNames) {
			TableDef table = sqlConnection.getTable(tableName).getTableDefinition();
			if (handleLinkTables && isLinkTable(table)) {
				Iterator<ForeignKey> it = table.getForeignKeys().iterator();
				ForeignKey fk1 = it.next();
				ForeignKey fk2 = it.next();
				TableName referencedTable1 = fk1.getReferencedTable();
				TableName referencedTable2 = fk2.getReferencedTable();
				if (!filter.matches(referencedTable1) || 
						!filter.matchesAll(referencedTable1, fk1.getLocalColumns()) || 
						!filter.matchesAll(referencedTable1, fk1.getReferencedColumns()) ||
						!filter.matches(referencedTable2) || 
						!filter.matchesAll(referencedTable2, fk2.getLocalColumns()) || 
						!filter.matchesAll(referencedTable2, fk2.getReferencedColumns())) {
					log.info("Skipping link table " + Microsyntax.toString(table.getName()));
					continue;
				}
				getPropertyBridge(table.getName(), fk1, fk2);
			} else {
				mapping.addClassMap(getClassMap(table, database));
			}
			if (tableName.getSchema() != null) {
				tryRegisterPrefix(tableName.getSchema().getName().toLowerCase(),
						style.getTableClass(tableName).getNameSpace());
			}
			if (tableName.getCatalog() != null) {
				tryRegisterPrefix(tableName.getCatalog().getName().toLowerCase(),
						style.getTableClass(tableName).getNameSpace());
			}
		}
		for (Entry<PropertyBridge,TableName> entry: refersToClassMaps.entrySet()) {
			entry.getKey().setRefersToClassMap(mapping.classMap(getClassMapResource(entry.getValue())));
		}
		for (Entry<PropertyBridge,TableName> entry: belongsToClassMaps.entrySet()) {
			entry.getKey().setBelongsToClassMap(mapping.classMap(getClassMapResource(entry.getValue())));
		}
		if (!tablesWithoutUniqueKey.isEmpty()) {
			StringBuilder s = new StringBuilder();
			s.append("Sorry, I don't know which columns to put into the ");
			s.append("d2rq:uriPattern of tables that don't have a ");
			s.append("primary or unique key. Please specify them manually: ");
			Iterator<TableName> it = tablesWithoutUniqueKey.iterator();
			while (it.hasNext()) {
				s.append(Microsyntax.toString(it.next()));
				if (it.hasNext()) {
					s.append(", ");
				}
			}
			if (!suppressWarnings) {
				log.warn(s.toString());
			}
		}
		log.info("Done!");
		this.finished = true;
	}

	private void tryRegisterPrefix(String prefix, String uri) {
		if (mapping.getPrefixes().getNsPrefixMap().containsKey(prefix)) return;
		if (mapping.getPrefixes().getNsPrefixMap().containsValue(uri)) return;
		try {
			mapping.getPrefixes().setNsPrefix(prefix, uri);
		} catch (IllegalPrefixException ex) {
			// Oh well, no prefix then.
		}
	}

	private Configuration getConfiguration() {
		Configuration result = new Configuration(
				model.createResource(mapNamespaceURI + "configuration"));
		result.setServeVocabulary(serveVocabulary);
		return result;
	}

	private Database getDatabase() {
		Database result = new Database(
				model.createResource(mapNamespaceURI + "database"));
		result.setJDBCDriver(sqlConnection.getJdbcDriverClass());
		result.setJdbcURL(sqlConnection.getJdbcURL());
		result.setUsername(sqlConnection.getUsername());
		result.setPassword(sqlConnection.getPassword());
		if (startupSQLScript != null) {
			result.setStartupSQLScript(model.createResource(startupSQLScript.toString()));
		}
		Properties props = sqlConnection.vendor().getDefaultConnectionProperties();
		for (String property: props.stringPropertyNames()) {
			result.setConnectionProperty(property, props.getProperty(property));
		}
		return result;
	}
	
	private ClassMap getClassMap(TableDef table, Database database) {
		log.info("Generating d2rq:ClassMap instance for table " + Microsyntax.toString(table.getName()));
		ClassMap result = new ClassMap(getClassMapResource(table.getName()));
		result.setComment("Table " + Microsyntax.toString(table.getName()));
		result.setDatabase(database);
		Key key = findBestKey(table);
		if (key == null) {
			if (style.getEntityPseudoKeyColumns(table.getColumns()) == null) {
				result.setComment(result.getComment() + 
						"\nNOTE: Sorry, I don't know which columns to put into the d2rq:uriPattern\n" +
						"because the table doesn't have a primary key. Please specify it manually.");
				result.setURIPattern(Microsyntax.toString(
						style.getEntityIRIPattern(table, null)));
				tablesWithoutUniqueKey.add(table.getName());
			} else {
				result.setBNodeIdColumns(table.getName().qualifyIdentifiers(
						style.getEntityPseudoKeyColumns(table.getColumns())));
			}
		} else {
			result.setURIPattern(Microsyntax.toString(
					style.getEntityIRIPattern(table, key)));
		}
		if (generateClasses) {
			result.addClass(style.getTableClass(table.getName()));
			if (generateDefinitionLabels) {
				result.addDefinitionLabel(model.createLiteral(
						Microsyntax.toString(table.getName())));
			}
		}
		if (generateLabelBridges && key != null) {
			getLabelBridge(table.getName(), key).setBelongsToClassMap(result);
		}
		Collection<ForeignKey> foreignKeys = table.getForeignKeys();
		for (Identifier column: table.getColumnNames()) {
			if (skipForeignKeyTargetColumns && isInForeignKey(column, table)) continue;
			if (!filter.matches(table.getName(), column)) {
				log.info("Skipping filtered column " + column);
				continue;
			}
			DataType type = table.getColumnDef(column).getDataType();
			if (type == null) {
				String message = "Skipping column " + column + ". Its datatype is unknown to D2RQ.\n";
				message += "You can override the column's datatype using d2rq:xxxColumn and add a property bridge.";
				if (!suppressWarnings) {
					log.warn(message);
				}
				result.setComment(result.getComment() + "\n" + message);
				continue;
			}
			if (type.isUnsupported()) {
				String message = "Skipping column " + column + ". Its datatype " + type + " cannot be mapped to RDF.";
				if (!suppressWarnings) {
					log.warn(message);
				}
				result.setComment(result.getComment() + "\n" + message);
				continue;
			}
			getPropertyBridge(table.getName(), column, table.getColumnDef(column).getDataType()).setBelongsToClassMap(result);
		}
		for (ForeignKey fk: foreignKeys) {
			if (!filter.matches(fk.getReferencedTable()) || 
					!filter.matchesAll(table.getName(), fk.getLocalColumns()) || 
					!filter.matchesAll(fk.getReferencedTable(), fk.getReferencedColumns())) {
				log.info("Skipping foreign key: " + fk);
				continue;
			}
			getPropertyBridge(table.getName(), fk).setBelongsToClassMap(result);
		}
		createVocabularyClass(table.getName());
		return result;
	}

	private PropertyBridge getLabelBridge(TableName table, Key columns) {
		PropertyBridge bridge = new PropertyBridge(getLabelBridgeResource(table));
		bridge.addProperty(RDFS.label);
		bridge.setPattern(Microsyntax.toString(style.getEntityLabelPattern(table, columns)));
		return bridge;
	}
	
	private PropertyBridge getPropertyBridge(TableName table, Identifier column, DataType datatype) {
		PropertyBridge bridge = new PropertyBridge(getPropertyBridgeResource(table, column));
		bridge.addProperty(style.getColumnProperty(table, column));
		if (generateDefinitionLabels) {
			bridge.addDefinitionLabel(model.createLiteral(
					table.getTable().getName() + " " + column.getName()));
		}
		bridge.setColumn(table.qualifyIdentifier(column));
		String datatypeURI = datatype.rdfType();
		if (datatypeURI != null && !XSD.xstring.getURI().equals(datatypeURI)) {
			// We use plain literals instead of xsd:strings, so skip
			// this if it's an xsd:string
			bridge.setDatatype(datatypeURI);
		}
		if (datatype.valueRegex() != null) {
			bridge.addValueRegex(datatype.valueRegex());
		}
		if (datatypeURI == null) {
			createDatatypeProperty(table, column, null);
		} else {
			createDatatypeProperty(table, column, TypeMapper.getInstance().getSafeTypeByName(datatypeURI));
		}
		tryRegisterPrefix(table.getTable().getName().toLowerCase(), 
				style.getColumnProperty(table, column).getNameSpace());
		return bridge;
	}

	private PropertyBridge getPropertyBridge(TableName table, ForeignKey foreignKey) {
		Key localColumns = foreignKey.getLocalColumns();
		TableName referencedTable = foreignKey.getReferencedTable();
		PropertyBridge bridge = new PropertyBridge(
				getPropertyBridgeResource(table, localColumns));
		bridge.addProperty(style.getForeignKeyProperty(table, foreignKey));
		refersToClassMaps.put(bridge, referencedTable);
		TableName target = referencedTable;
		if (referencedTable.equals(table)) {
			// Same-table join? Then we need to set up an alias for the table and join to that
			target = TableName.create(null, null, 
					Identifier.createDelimited( 
							Microsyntax.toString(referencedTable).replace('.', '_') + "__alias"));
			bridge.addAlias(new AliasDeclaration(referencedTable, target));
			foreignKey = new ForeignKey(foreignKey.getLocalColumns(), foreignKey.getReferencedColumns(), target);
		}
		for (Join join: Join.createFrom(table, foreignKey)) {
			bridge.addJoin(join);
		}
		createObjectProperty(table, foreignKey);
		return bridge;
	}

	private PropertyBridge getPropertyBridge(TableName table, 
			ForeignKey fk1, ForeignKey fk2) {
		TableName referencedTable1 = fk1.getReferencedTable();
		TableName referencedTable2 = fk2.getReferencedTable();
		log.info("Generating d2rq:PropertyBridge instance for table " + Microsyntax.toString(table));
		PropertyBridge bridge = new PropertyBridge(getLinkPropertyBridgeResource(table));
		boolean isSelfJoin = referencedTable1.equals(referencedTable2);
		bridge.setComment("Table " + Microsyntax.toString(table) + (isSelfJoin ? " (n:m self-join)" : " (n:m)"));
		belongsToClassMaps.put(bridge, referencedTable1);
		bridge.addProperty(style.getLinkProperty(table));
		refersToClassMaps.put(bridge, referencedTable2);
		TableName target = referencedTable2;
		if (isSelfJoin) {
			target = TableName.create(null, null, 
					Identifier.createDelimited( 
							Microsyntax.toString(referencedTable2).replace('.', '_') +
							Microsyntax.toString(table).replace('.', '_') +
							"__alias"));
			bridge.addAlias(new AliasDeclaration(referencedTable2, target)); 
			fk2 = new ForeignKey(fk2.getLocalColumns(), fk2.getReferencedColumns(), target);
		}
		for (Join join: Join.createFrom(table, fk1)) {
			bridge.addJoin(join);
		}
		for (Join join: Join.createFrom(table, fk2)) {
			bridge.addJoin(join);
		}
		createLinkProperty(table, referencedTable1, referencedTable2);
		return bridge;
	}

	private Resource getClassMapResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table)));
	}
	
	private Resource getLabelBridgeResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table) + "__label"));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Identifier column) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table, column)));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Key columns) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table, columns) + "__ref"));
	}
	
	private Resource getLinkPropertyBridgeResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table) + "__link"));
	}
	
	private Key findBestKey(TableDef table) {
		if (table.getPrimaryKey() != null) {
			if (!isFiltered(table, table.getPrimaryKey(), true)) {
				return table.getPrimaryKey();
			}
		}
		if (!useUniqueKeysAsEntityID) return null;
		for (Key uniqueKey: table.getUniqueKeys()) {
			if (!isFiltered(table, uniqueKey, true)) {
				return uniqueKey;
			}
		}
		return null;
	}
	
	private boolean isFiltered(TableDef table, Key columns, boolean requireDistinct) {
		for (Identifier column: columns) {
			if (isFiltered(table, column, requireDistinct)) return true;
		}
		return false;
	}
	
	private boolean isFiltered(TableDef table, Identifier column, boolean requireDistinct) {
		if (!filter.matches(table.getName(), column)) return true;
		DataType type = table.getColumnDef(column).getDataType();
		return type == null || type.isUnsupported() || (requireDistinct && !type.supportsDistinct());
	}

	/**
	 * A table T is considered to be a link table if it has exactly two
	 * foreign key constraints, and the constraints reference other
	 * tables (not T), and the constraints cover all columns of T,
	 * and there are no foreign keys from other tables pointing to this table
	 */
	private boolean isLinkTable(TableDef table) {
		if (table.getForeignKeys().size() != 2) return false;
		if (sqlConnection.isReferencedByForeignKey(table.getName())) return false;
		List<Identifier> columns = new ArrayList<Identifier>();
		for (ColumnDef qualified: table.getColumns()) {
			columns.add(qualified.getName());
		}
		for (ForeignKey foreignKey: table.getForeignKeys()) {
			if (foreignKey.getReferencedTable().equals(table.getName())) return false;
			columns.removeAll(foreignKey.getLocalColumns().getColumns());
		}
		return columns.isEmpty();
	}

	/**
	 * @return <code>true</code> iff the table contains this column as a local column in a foreign key
	 */
	private boolean isInForeignKey(Identifier column, TableDef table) {
		for (ForeignKey fk: table.getForeignKeys()) {
			if (fk.getLocalColumns().contains(column)) return true;
		}
		return false;
	}
	
	private void initVocabularyModel() {
		vocabModel.setNsPrefixes(model.getNsPrefixMap());
		vocabModel.setNsPrefix("rdf", RDF.getURI());
		vocabModel.setNsPrefix("rdfs", RDFS.getURI());
		vocabModel.setNsPrefix("owl", OWL.getURI());
		vocabModel.setNsPrefix("dc", DC.getURI());
		vocabModel.setNsPrefix("xsd", XSD.getURI());
		Resource r = style.getGeneratedOntologyResource();
		r.addProperty(RDF.type, OWL.Ontology);
		r.addProperty(OWL.imports, this.vocabModel.getResource(dropTrailingHash(DC.getURI())));
		r.addProperty(DC.creator, CREATOR);
	}

	private void createVocabularyClass(TableName tableName) {
		Resource r = style.getTableClass(tableName);
		r.addProperty(RDF.type, RDFS.Class);
		r.addProperty(RDF.type, OWL.Class);
		r.addProperty(RDFS.label, Microsyntax.toString(tableName));
		r.addProperty(RDFS.isDefinedBy, style.getGeneratedOntologyResource());
	}

	private void createDatatypeProperty(TableName tableName, Identifier column, RDFDatatype datatype) {
		Resource r = style.getColumnProperty(tableName, column);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, stringMaker.toString(tableName, column));
		r.addProperty(RDFS.domain, style.getTableClass(tableName));
		r.addProperty(RDFS.isDefinedBy, style.getGeneratedOntologyResource());		
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(TableName tableName, ForeignKey fk) {
		Resource r = style.getForeignKeyProperty(tableName, fk);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, stringMaker.toString(tableName, fk.getLocalColumns()));
		r.addProperty(RDFS.domain, style.getTableClass(tableName));
		r.addProperty(RDFS.range, style.getTableClass(fk.getReferencedTable()));
		r.addProperty(RDFS.isDefinedBy, style.getGeneratedOntologyResource());		
	}
	
	private void createLinkProperty(TableName linkTableName, TableName fromTable, TableName toTable) {
		Resource r = style.getLinkProperty(linkTableName);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, Microsyntax.toString(linkTableName));
		r.addProperty(RDFS.domain, style.getTableClass(fromTable));
		r.addProperty(RDFS.range, style.getTableClass(toTable));
		r.addProperty(RDFS.isDefinedBy, style.getGeneratedOntologyResource());
	}
	
	public static String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}
