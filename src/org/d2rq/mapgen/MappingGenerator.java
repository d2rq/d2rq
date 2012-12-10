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
import org.d2rq.db.schema.ColumnName;
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
import org.d2rq.values.TemplateValueMaker;
import org.d2rq.vocab.D2RQ;
import org.d2rq.vocab.JDBC;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
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

	private final SQLConnection sqlConnection;
	private Mapping mapping = null;
	protected Model mappingResources = null;
	private String mapNamespaceURI;
	protected String instanceNamespaceURI;
	protected String vocabNamespaceURI;
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
	private final Map<String,Object> assignedNames = 
			new HashMap<String,Object>();
	private final List<TableName> tablesWithoutUniqueKey = 
			new ArrayList<TableName>();
	private final Map<PropertyBridge,TableName> refersToClassMaps = 
			new HashMap<PropertyBridge,TableName>();
	private final Map<PropertyBridge,TableName> belongsToClassMaps = 
			new HashMap<PropertyBridge,TableName>();
	
	public MappingGenerator(SQLConnection sqlConnection) {
		this.sqlConnection = sqlConnection;
		mapNamespaceURI = "#";
		instanceNamespaceURI = "";
		vocabNamespaceURI = "vocab/";
	}

	public void setMapNamespaceURI(String uri) {
		this.mapNamespaceURI = uri;
	}
	
	public void setInstanceNamespaceURI(String uri) {
		this.instanceNamespaceURI = uri;
	}

	public void setVocabNamespaceURI(String uri) {
		this.vocabNamespaceURI = uri;
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
		mappingResources = ModelFactory.createDefaultModel();
		mapping = new Mapping();
		mappingResources.setNsPrefixes(mapping.getPrefixes());
		mappingResources.setNsPrefix("map", mapNamespaceURI);
		mappingResources.setNsPrefix("", instanceNamespaceURI);
		if (!vocabNamespaceURI.equals(instanceNamespaceURI)) {
			mappingResources.setNsPrefix("vocab", vocabNamespaceURI);
		}
		mappingResources.setNsPrefix("rdf", RDF.getURI());
		mappingResources.setNsPrefix("rdfs", RDFS.getURI());
		mappingResources.setNsPrefix("xsd", XSD.getURI());
		mappingResources.setNsPrefix("d2rq", D2RQ.getURI());
		mappingResources.setNsPrefix("jdbc", JDBC.getURI());
		mapping.getPrefixes().setNsPrefixes(mappingResources);
		log.info("Generating d2rq:Configuration instance");
		mapping.setConfiguration(getConfiguration());
		log.info("Generating d2rq:Database instance");
		Database database = getDatabase();
		mapping.addDatabase(database);
		initVocabularyModel();
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

	private Configuration getConfiguration() {
		Configuration result = new Configuration(
				mappingResources.createResource(mapNamespaceURI + "configuration"));
		result.setServeVocabulary(serveVocabulary);
		return result;
	}

	private Database getDatabase() {
		Database result = new Database(
				mappingResources.createResource(mapNamespaceURI + "database"));
		result.setJDBCDriver(sqlConnection.getJdbcDriverClass());
		result.setJdbcURL(sqlConnection.getJdbcURL());
		result.setUsername(sqlConnection.getUsername());
		result.setPassword(sqlConnection.getPassword());
		if (startupSQLScript != null) {
			result.setStartupSQLScript(mappingResources.createResource(startupSQLScript.toString()));
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
			definePseudoEntityIdentifier(result, table);
		} else {
			result.setURIPattern(Microsyntax.toString(
					getEntityIdentifierPattern(table, key)));
		}
		if (generateClasses) {
			result.addClass(getTableClass(table.getName()));
			if (generateDefinitionLabels) {
				result.addDefinitionLabel(mappingResources.createLiteral(
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

	protected void definePseudoEntityIdentifier(ClassMap result, TableDef table) {
		result.setComment(result.getComment() + 
				"\nNOTE: Sorry, I don't know which columns to put into the d2rq:uriPattern\n" +
				"because the table doesn't have a primary key. Please specify it manually.");
		result.setURIPattern(Microsyntax.toString(
				getEntityIdentifierPattern(table, null)));
		tablesWithoutUniqueKey.add(table.getName());
	}
	
	private PropertyBridge getLabelBridge(TableName table, Key columns) {
		PropertyBridge bridge = new PropertyBridge(getLabelBridgeResource(table));
		bridge.addProperty(RDFS.label);
		bridge.setPattern(Microsyntax.toString(getLabelPattern(table, columns)));
		return bridge;
	}
	
	private PropertyBridge getPropertyBridge(TableName table, Identifier column, DataType datatype) {
		PropertyBridge bridge = new PropertyBridge(getPropertyBridgeResource(table, column));
		bridge.addProperty(getColumnProperty(table, column));
		if (generateDefinitionLabels) {
			bridge.addDefinitionLabel(mappingResources.createLiteral(
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
		return bridge;
	}

	private PropertyBridge getPropertyBridge(TableName table, ForeignKey foreignKey) {
		Key localColumns = foreignKey.getLocalColumns();
		TableName referencedTable = foreignKey.getReferencedTable();
		PropertyBridge bridge = new PropertyBridge(
				getPropertyBridgeResource(table, localColumns));
		bridge.addProperty(getForeignKeyProperty(table, localColumns));
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
		bridge.addProperty(getLinkProperty(table));
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

	/**
	 * Returns SCHEMA_TABLE. Except if that string is already taken
	 * by another table name (or column name); in that case we add
	 * more underscores until we have no clash.
	 */
	private String toUniqueString(TableName tableName) {
		if (tableName.getSchema() == null) {
			return tableName.getTable().getName();
		}
		String separator = "_";
		while (true) {
			String candidate = tableName.getSchema().getName() + separator + tableName.getTable().getName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, tableName);
				return candidate;
			}
			if (assignedNames.get(candidate).equals(tableName)) {
				return candidate;
			}
			separator += "_";
		}
	}
	
	/**
	 * Returns TABLE_COLUMN. Except if that string is already taken by
	 * another column name (e.g., AAA.BBB_CCC and AAA_BBB.CCC would
	 * result in the same result AAA_BBB_CCC); in that case we add more
	 * underscores (AAA__BBB_CCC) until we have no clash. 
	 */
	private String toUniqueString(TableName tableName, Identifier column) {
		String separator = "_";
		while (true) {
			String candidate = toUniqueString(tableName) + separator + column.getName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, ColumnName.create(tableName, column));
				return candidate;
			}
			if (assignedNames.get(candidate).equals(ColumnName.create(tableName, column))) {
				return candidate;
			}
			separator += "_";
		}
	}

	private String toUniqueString(TableName tableName, Key columns) {
		StringBuffer result = new StringBuffer();
		result.append(toUniqueString(tableName));
		for (Identifier column: columns.getColumns()) {
			result.append("_" + column.getName());
		}
		return result.toString();
	}

	private Resource getClassMapResource(TableName table) {
		return mappingResources.createResource(mapNamespaceURI + 
				IRIEncoder.encode(toUniqueString(table)));
	}
	
	private Resource getLabelBridgeResource(TableName table) {
		return mappingResources.createResource(mapNamespaceURI + 
				IRIEncoder.encode(toUniqueString(table) + "__label"));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Identifier column) {
		return mappingResources.createResource(mapNamespaceURI + 
				IRIEncoder.encode(toUniqueString(table, column)));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Key columns) {
		return mappingResources.createResource(mapNamespaceURI + 
				IRIEncoder.encode(toUniqueString(table, columns) + "__ref"));
	}
	
	private Resource getLinkPropertyBridgeResource(TableName table) {
		return mappingResources.createResource(mapNamespaceURI + 
				IRIEncoder.encode(toUniqueString(table) + "__link"));
	}
	
	protected Resource getTableClass(TableName tableName) {
		return mappingResources.createResource(
				vocabNamespaceURI + IRIEncoder.encode(toUniqueString(tableName)));
	}
	
	protected Property getColumnProperty(TableName tableName, Identifier column) {
		return mappingResources.createProperty(
				vocabNamespaceURI + IRIEncoder.encode(toUniqueString(tableName, column)));
	}
	
	protected Property getForeignKeyProperty(TableName tableName, Key columns) {
		return mappingResources.createProperty(
				vocabNamespaceURI + IRIEncoder.encode(toUniqueString(tableName, columns)));
	}

	private Property getLinkProperty(TableName linkTable) {
		return mappingResources.createProperty(
				vocabNamespaceURI + IRIEncoder.encode(toUniqueString(linkTable)));
	}
	
	private Resource getOntologyResource() {
		return mappingResources.createResource(dropTrailingHash(vocabNamespaceURI));
	}
	
	protected TemplateValueMaker getEntityIdentifierPattern(TableDef table, Key columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		builder.add(instanceNamespaceURI);
		if (table.getName().getSchema() != null) {
			builder.add(IRIEncoder.encode(table.getName().getSchema().getName()));
			builder.add("/");
		}
		builder.add(IRIEncoder.encode(table.getName().getTable().getName()));
		if (columns != null) {
			for (Identifier column: columns) {
				builder.add("/");
				if (table.getColumnDef(column).getDataType().isIRISafe()) {
					builder.add(table.getName().qualifyIdentifier(column));
				} else {
					builder.add(table.getName().qualifyIdentifier(column), TemplateValueMaker.URLIFY);
				}
			}
		}
		return builder.build();
	}
	
	private TemplateValueMaker getLabelPattern(TableName tableName, Key columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		builder.add(tableName.getTable().getName());
		builder.add(" #");
		Iterator<Identifier> it = columns.iterator();
		while (it.hasNext()) {
			builder.add(tableName.qualifyIdentifier(it.next()));
			if (it.hasNext()) {
				builder.add("/");
			}
		}
		return builder.build();
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
	
	protected boolean isFiltered(TableDef table, Identifier column, boolean requireDistinct) {
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
		this.vocabModel.setNsPrefix("rdf", RDF.getURI());
		this.vocabModel.setNsPrefix("rdfs", RDFS.getURI());
		this.vocabModel.setNsPrefix("owl", OWL.getURI());
		this.vocabModel.setNsPrefix("dc", DC.getURI());
		this.vocabModel.setNsPrefix("xsd", XSDDatatype.XSD + "#");
		this.vocabModel.setNsPrefix("", this.vocabNamespaceURI);
		Resource r = getOntologyResource();
		r.addProperty(RDF.type, OWL.Ontology);
		r.addProperty(OWL.imports, this.vocabModel.getResource(dropTrailingHash(DC.getURI())));
		r.addProperty(DC.creator, CREATOR);
	}

	private void createVocabularyClass(TableName tableName) {
		Resource r = getTableClass(tableName);
		r.addProperty(RDF.type, RDFS.Class);
		r.addProperty(RDF.type, OWL.Class);
		r.addProperty(RDFS.label, Microsyntax.toString(tableName));
		r.addProperty(RDFS.isDefinedBy, getOntologyResource());
	}

	private void createDatatypeProperty(TableName tableName, Identifier column, RDFDatatype datatype) {
		Resource r = getColumnProperty(tableName, column);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, toUniqueString(tableName, column));
		r.addProperty(RDFS.domain, getTableClass(tableName));
		r.addProperty(RDFS.isDefinedBy, getOntologyResource());		
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(TableName tableName, ForeignKey fk) {
		Resource r = getForeignKeyProperty(tableName, fk.getLocalColumns());
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, toUniqueString(tableName, fk.getLocalColumns()));
		r.addProperty(RDFS.domain, getTableClass(tableName));
		r.addProperty(RDFS.range, getTableClass(fk.getReferencedTable()));
		r.addProperty(RDFS.isDefinedBy, getOntologyResource());		
	}
	
	private void createLinkProperty(TableName linkTableName, TableName fromTable, TableName toTable) {
		String propertyURI = this.vocabNamespaceURI + IRIEncoder.encode(linkTableName.getTable().getName());
		Resource r = this.vocabModel.createResource(propertyURI);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, Microsyntax.toString(linkTableName));
		r.addProperty(RDFS.domain, getTableClass(fromTable));
		r.addProperty(RDFS.range, getTableClass(toTable));
		r.addProperty(RDFS.isDefinedBy, getOntologyResource());
	}
	
	private String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}
