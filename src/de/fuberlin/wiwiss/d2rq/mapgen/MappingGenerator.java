package de.fuberlin.wiwiss.d2rq.mapgen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;

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
	private final static OutputStream DUMMY_STREAM = 
			new OutputStream() { public void write(int b) {}};

	protected final ConnectedDB database;
	protected final DatabaseSchemaInspector schema;
	private String mapNamespaceURI;
	protected String instanceNamespaceURI;
	private String vocabNamespaceURI;
	private String driverClass = null;
	private Filter filter = Filter.ALL;
	protected PrintWriter out = null;
	private Model vocabModel = ModelFactory.createDefaultModel();
	private boolean finished = false;
	private boolean generateClasses = true;
	private boolean generateLabelBridges = true;
	private boolean generateDefinitionLabels = true;
	private boolean handleLinkTables = true;
	private boolean serveVocabulary = true;
	private boolean skipForeignKeyTargetColumns = true;
	private URI startupSQLScript;
	private Map<String,Object> assignedNames = new HashMap<String,Object>();
	
	public MappingGenerator(ConnectedDB database) {
		this.database = database;
		schema = database.schemaInspector();
		mapNamespaceURI = "#";
		instanceNamespaceURI = "";
		vocabNamespaceURI = "vocab/";
		driverClass = ConnectedDB.guessJDBCDriverClass(database.getJdbcURL());
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
	
	public void setJDBCDriverClass(String driverClassName) {
		this.driverClass = driverClassName;
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
	
	public Model vocabularyModel() {
		if (!this.finished) {
			writeMapping(DUMMY_STREAM);
		}
		return this.vocabModel;
	}
	
	/**
	 * Returns an in-memory Jena model containing the D2RQ mapping.
	 * 
	 * @param baseURI Base URI for resolving relative URIs in the mapping, e.g., map namespace
	 * @return In-memory Jena model containing the D2RQ mapping
	 */
	public Model mappingModel(String baseURI) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeMapping(out);
		try {
			String mappingAsTurtle = out.toString("UTF-8");
			Model result = ModelFactory.createDefaultModel();
			result.read(new StringReader(mappingAsTurtle), baseURI, "TURTLE");
			return result;
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("UTF-8 is always supported");
		}
	}
	
	public void writeMapping(OutputStream out) {
		try {
			Writer w = new OutputStreamWriter(out, "UTF-8");
			writeMapping(w);
			w.flush();			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Writes the D2RQ mapping to a writer. Note that the calling code is
	 * responsible for closing the writers after use.
	 * 
	 * @param out Stream for writing data to the file or console
	 */
	public void writeMapping(Writer out) {
		this.out = new PrintWriter(out);
		this.out.println("@prefix map: <" + this.mapNamespaceURI + "> .");
		this.out.println("@prefix db: <" + this.instanceNamespaceURI + "> .");
		this.out.println("@prefix vocab: <" + this.vocabNamespaceURI + "> .");
		this.out.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		this.out.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		this.out.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		this.out.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		this.out.println("@prefix jdbc: <http://d2rq.org/terms/jdbc/> .");
		this.out.println();
		if (!serveVocabulary) {
			writeConfiguration();
		}
		writeDatabase();
		initVocabularyModel();
		List<RelationName> tableNames = new ArrayList<RelationName>();
		for (RelationName tableName: schema.listTableNames(filter.getSingleSchema())) {
			if (!filter.matches(tableName)) {
				log.info("Skipping table " + tableName);
				continue;
			}
			tableNames.add(tableName);
		}
		log.info("Filter '" + filter + "' matches " + tableNames.size() + " total tables");
		for (RelationName tableName: tableNames) {
			if (handleLinkTables && isLinkTable(tableName)) {
				writeLinkTable(tableName);
			} else {
				writeTable(tableName);
			}
		}
		log.info("Done!");
		this.out.flush();
		this.out = null;
		this.finished = true;
	}

	private void writeConfiguration() {
		log.info("Generating d2rq:Configuration instance");
		this.out.println(configIRITurtle() + " a d2rq:Configuration;");
		this.out.println("\td2rq:serveVocabulary false.");
		this.out.println();
	}

	private void writeDatabase() {
		log.info("Generating d2rq:Database instance");
		this.out.println(databaseIRITurtle() + " a d2rq:Database;");
		this.out.println("\td2rq:jdbcDriver \"" + this.driverClass + "\";");
		this.out.println("\td2rq:jdbcDSN \"" + database.getJdbcURL() + "\";");
		if (database.getUsername() != null) {
			this.out.println("\td2rq:username \"" + database.getUsername() + "\";");
		}
		if (database.getPassword() != null) {
			this.out.println("\td2rq:password \"" + database.getPassword() + "\";");
		}
		if (startupSQLScript != null) {
			out.println("\td2rq:startupSQLScript <" + startupSQLScript + ">;");
		}
		Properties props = database.vendor().getDefaultConnectionProperties();
		for (Object property: props.keySet()) {
			String value = props.getProperty((String) property);
			this.out.println("\tjdbc:" + property + " \"" + value + "\";");
		}
		this.out.println("\t.");
		this.out.println();
		this.out.flush(); // Let a tail see that we're working
	}
	
	public void writeTable(RelationName tableName) {
		log.info("Generating d2rq:ClassMap instance for table " + tableName.qualifiedName());
		this.out.println("# Table " + tableName);
		List<Attribute> identifierColumns = identifierColumns(tableName);
		boolean hasIdentifier = !identifierColumns.isEmpty();
		this.out.println(classMapIRITurtle(tableName) + " a d2rq:ClassMap;");
		this.out.println("\td2rq:dataStorage " + databaseIRITurtle() + ";");
		if (hasIdentifier) {
			writeEntityIdentifier(tableName, identifierColumns);
		} else {
			writePseudoEntityIdentifier(tableName);
		}

		if (generateClasses) {
			this.out.println("\td2rq:class " + vocabularyIRITurtle(tableName) + ";");
			if (generateDefinitionLabels) {
				this.out.println("\td2rq:classDefinitionLabel \"" + tableName + "\";");
			}
		}
		this.out.println("\t.");
		if (generateLabelBridges && hasIdentifier) {
			writeLabelBridge(tableName, identifierColumns);
		}
		List<Join> foreignKeys = schema.foreignKeys(tableName, DatabaseSchemaInspector.KEYS_IMPORTED);
		for (Attribute column: filter(schema.listColumns(tableName), false, "property bridge")) {
			if (skipForeignKeyTargetColumns && isInForeignKey(column, foreignKeys)) continue;
			writeColumn(column);
		}
		for (Join fk: foreignKeys) {
			if (!filter.matches(fk.table1()) || !filter.matches(fk.table2()) || 
					!filter.matchesAll(fk.attributes1()) || !filter.matchesAll(fk.attributes2())) {
				log.info("Skipping foreign key: " + fk);
				continue;
			}
			writeForeignKey(fk);
		}
		createVocabularyClass(tableName);
		this.out.println();
		// Let a tail see that we're working and ensure that every table that did
		// NOT generate an exception is actually saved...
		this.out.flush();
	}

	protected void writeEntityIdentifier(RelationName tableName, 
			List<Attribute> identifierColumns) {
		String uriPattern = this.instanceNamespaceURI;
		if (tableName.schemaName() != null) {
			uriPattern += IRIEncoder.encode(tableName.schemaName()) + "/";
		}
		uriPattern += IRIEncoder.encode(tableName.tableName());
		for (Attribute column: identifierColumns) {
			uriPattern += "/@@" + column.qualifiedName();
			if (!schema.columnType(column).isIRISafe()) {
				uriPattern += "|urlify";
			}
			uriPattern += "@@";
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern + "\";");
	}
	
	protected void writePseudoEntityIdentifier(RelationName tableName) {
		writeWarning(new String[]{
				"Sorry, I don't know which columns to put into the uriPattern",
				"    for \"" + tableName + "\" because the table doesn't have a primary key.", 
				"    Please specify it manually."
			}, "\t");
		writeEntityIdentifier(tableName, Collections.<Attribute>emptyList());
	}
	
	public void writeLabelBridge(RelationName tableName, List<Attribute> labelColumns) {
		this.out.println(propertyBridgeIRITurtle(tableName, "label") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(tableName) + ";");
		this.out.println("\td2rq:property rdfs:label;");
		this.out.println("\td2rq:pattern \"" + labelPattern(tableName.tableName(), labelColumns) + "\";");
		this.out.println("\t.");
	}
	
	public void writeColumn(Attribute column) {
		this.out.println(propertyBridgeIRITurtle(column) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(column.relationName()) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(column) + ";");
		if (generateDefinitionLabels) {
			this.out.println("\td2rq:propertyDefinitionLabel \"" + toLabel(column) + "\";");
		}
		this.out.println("\td2rq:column \"" + column.qualifiedName() + "\";");
		DataType colType = this.schema.columnType(column);
		String xsd = colType.rdfType();
		if (xsd != null && !"xsd:string".equals(xsd)) {
			// We use plain literals instead of xsd:strings, so skip
			// this if it's an xsd:string
			this.out.println("\td2rq:datatype " + xsd + ";");
		}
		if (colType.valueRegex() != null) {
			this.out.println("\td2rq:valueRegex \"" + colType.valueRegex() + "\";");
		}
		if (xsd == null) {
			createDatatypeProperty(column, null);
		} else {
			String datatypeURI = xsd.replaceAll("xsd:", XSDDatatype.XSD + "#");
			createDatatypeProperty(column, TypeMapper.getInstance().getSafeTypeByName(datatypeURI));
		}
		this.out.println("\t.");
	}

	public void writeForeignKey(Join foreignKey) {
		RelationName primaryTable = schema.getCorrectCapitalization(foreignKey.table1());
		List<Attribute> primaryColumns = foreignKey.attributes1();
		RelationName foreignTable = schema.getCorrectCapitalization(foreignKey.table2());
		this.out.println(propertyBridgeIRITurtle(primaryColumns, "ref") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(primaryTable) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(primaryColumns) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapIRITurtle(foreignTable) + ";");
		AliasMap alias = AliasMap.NO_ALIASES;
		// Same-table join? Then we need to set up an alias for the table and join to that
		if (foreignKey.isSameTable()) {
			String aliasName = foreignTable.qualifiedName().replace('.', '_') + "__alias";
			this.out.println("\td2rq:alias \"" + foreignTable.qualifiedName() + " AS " + aliasName + "\";");
			alias = AliasMap.create1(foreignTable, new RelationName(null, aliasName));
		}
		for (Attribute column: primaryColumns) {
			this.out.println("\td2rq:join \"" + column.qualifiedName() + " " + Join.joinOperators[foreignKey.joinDirection()] + " " +
					alias.applyTo(foreignKey.equalAttribute(column)).qualifiedName() + "\";");
		}
		createObjectProperty(foreignKey);
		this.out.println("\t.");
	}

	private void writeLinkTable(RelationName linkTableName) {
		List<Join> keys = schema.foreignKeys(linkTableName, DatabaseSchemaInspector.KEYS_IMPORTED);
		Join join1 = keys.get(0);
		Join join2 = keys.get(1);
		if (!filter.matches(join1.table1()) || !filter.matches(join1.table2()) || 
				!filter.matchesAll(join1.attributes1()) || !filter.matchesAll(join1.attributes2()) ||
				!filter.matches(join2.table1()) || !filter.matches(join2.table2()) || 
				!filter.matchesAll(join2.attributes1()) || !filter.matchesAll(join2.attributes2())) {
			log.info("Skipping link table " + linkTableName);
			return;
		}
		log.info("Generating d2rq:PropertyBridge instance for table " + linkTableName.qualifiedName());
		RelationName table1 = this.schema.getCorrectCapitalization(join1.table2());
		RelationName table2 = this.schema.getCorrectCapitalization(join2.table2());
		boolean isSelfJoin = table1.equals(table2);
		this.out.println("# Table " + linkTableName + (isSelfJoin ? " (n:m self-join)" : " (n:m)"));
		this.out.println(propertyBridgeIRITurtle(linkTableName, "link") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(table1) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(linkTableName) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapIRITurtle(table2) + ";");
		for (Attribute column: join1.attributes1()) {
			Attribute otherColumn = join1.equalAttribute(column);
			this.out.println("\td2rq:join \"" + column.qualifiedName() + " " + Join.joinOperators[join1.joinDirection()] + " " + otherColumn.qualifiedName() + "\";");
		}
		AliasMap alias = AliasMap.NO_ALIASES;
		if (isSelfJoin) {
			RelationName aliasName = new RelationName(
					null, table2.tableName() + "_" + linkTableName.tableName() + "__alias");
			alias = AliasMap.create1(table2, aliasName);
			this.out.println("\td2rq:alias \"" + table2.qualifiedName() + 
					" AS " + aliasName.qualifiedName() + "\";");
		}
		for (Attribute column: join2.attributes1()) {
			Attribute otherColumn = join2.equalAttribute(column);
			this.out.println("\td2rq:join \"" + column.qualifiedName() + " " + Join.joinOperators[join2.joinDirection()] + " " + alias.applyTo(otherColumn).qualifiedName() + "\";");
		}
		this.out.println("\t.");
		this.out.println();
		createLinkProperty(linkTableName, table1, table2);
		this.out.flush();
	}

	private void writeWarning(String[] warnings, String indent) {
		for (String warning: warnings) {
			this.out.println(indent + "# " + warning);
			log.warn(warning);
		}
	}
	
	/**
	 * Returns SCHEMA_TABLE. Except if that string is already taken
	 * by another table name (or column name); in that case we add
	 * more underscores until we have no clash.
	 */
	private String toUniqueString(RelationName table) {
		if (table.schemaName() == null) {
			return table.tableName();
		}
		String separator = "_";
		while (true) {
			String candidate = table.schemaName() + separator + table.tableName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, table);
				return candidate;
			}
			if (assignedNames.get(candidate).equals(table)) {
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
	private String toUniqueString(Attribute column) {
		String separator = "_";
		while (true) {
			String candidate = toUniqueString(column.relationName()) + separator + column.attributeName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, column);
				return candidate;
			}
			if (assignedNames.get(candidate).equals(column)) {
				return candidate;
			}
			separator += "_";
		}
	}

	private String toUniqueString(List<Attribute> columns) {
		StringBuffer result = new StringBuffer();
		result.append(toUniqueString((columns.get(0)).relationName()));
		for (Attribute column: columns) {
			result.append("_" + column.attributeName());
		}
		return result.toString();
	}

	/**
	 * Presents the IRI given by namespaceIRI + localName in Turtle-compatible
	 * syntax, either as prefix:localName (if possible), or the full IRI
	 * surrounded by pointy brackets.
	 * @param localName is IRI-encoded if necessary
	 */
	private String toTurtleIRI(String namespaceIRI, String prefix, String localName) {
		localName = IRIEncoder.encode(localName);
		if (turtleLocalName.matcher(localName).matches()) {
			return prefix + ":" + localName;
		}
		return "<" + namespaceIRI + localName + ">";
	}
	// A conservative pattern for the local part of a Turtle prefixed name.
	// If a URI doesn't match, better don't prefix-abbreviate it.
	// We could be more aggressive and allow certain Unicode chars and non-trailing periods. 
	private final static Pattern turtleLocalName = 
		Pattern.compile("([a-zA-Z_][a-zA-Z0-9_-]*)?");

	private String configIRITurtle() {
		return "map:Configuration";
	}

	private String databaseIRITurtle() {
		return "map:database";
	}
	
	private String classMapIRITurtle(RelationName tableName) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName));
	}

	private String propertyBridgeIRITurtle(RelationName tableName, String suffix) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName) + "__" + suffix);
	}

	private String propertyBridgeIRITurtle(Attribute attribute) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(attribute));
	}

	private String propertyBridgeIRITurtle(List<Attribute> attributes, String suffix) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(attributes) + "__" + suffix);
	}

	protected String vocabularyIRITurtle(RelationName tableName) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(tableName));
	}

	protected String vocabularyIRITurtle(Attribute attribute) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(attribute));
	}

	protected String vocabularyIRITurtle(List<Attribute> attributes) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(attributes));
	}
	
	private Resource ontologyResource() {
		String ontologyURI = dropTrailingHash(vocabNamespaceURI);
		return this.vocabModel.createResource(ontologyURI);
	}
	
	private Resource classResource(RelationName tableName) {
		String classURI = this.vocabNamespaceURI + toUniqueString(tableName);
		return this.vocabModel.createResource(classURI);		
	}

	private Resource propertyResource(Attribute column) {
		String propertyURI = this.vocabNamespaceURI + toUniqueString(column);
		return this.vocabModel.createResource(propertyURI);
	}
	
	private Resource propertyResource(List<Attribute> columns) {
		String propertyURI = this.vocabNamespaceURI + toUniqueString(columns);
		return this.vocabModel.createResource(propertyURI);
	}

	private String toLabel(Attribute column) {
		return column.tableName() + " " + column.attributeName();
	}

	private String labelPattern(String name, List<Attribute> labelColumns) {
		String result = name + " #";
		Iterator<Attribute> it = labelColumns.iterator();
		while (it.hasNext()) {
			result += "@@" + it.next().qualifiedName() + "@@";
			if (it.hasNext()) {
				result += "/";
			}
		}
		return result;
	}

	private List<Attribute> identifierColumns(RelationName tableName) {
		List<Attribute> columns = schema.primaryKeyColumns(tableName);
		if (filter.matchesAll(columns)) {
			return filter(columns, true, "identifier column");
		}
		return Collections.<Attribute>emptyList();
	}
	
	protected List<Attribute> filter(List<Attribute> columns, boolean requireDistinct, String reason) {
		List<Attribute> result = new ArrayList<Attribute>(columns.size());
		for (Attribute column: columns) {
			if (!filter.matches(column)) {
				log.info("Skipping filtered column " + column + " as " + reason);
				continue;
			}
			DataType type = schema.columnType(column);
			if (type == null) {
				writeWarning(new String[]{
						"Skipping column " + column + " as " + reason + ".",
						"    Its datatype is unknown to D2RQ.",
						"    You can override the column's datatype using d2rq:xxxColumn and add a property bridge.",
					}, "");
				continue;
			}
			if (type.isUnsupported()) {
				writeWarning(new String[]{
						"Skipping column " + column + " as " + reason + ".",
						"    Its datatype " + schema.columnType(column) + " cannot be mapped to RDF."
					}, "");
				continue;
			}
			if (requireDistinct && !type.supportsDistinct()) {
				writeWarning(new String[]{
						"Skipping column " + column + " as " + reason + ".",
						"    Its datatype " + schema.columnType(column) + " does not support DISTINCT."
				}, "");
			}
			result.add(column);
		}
		return result;
	}
	
	/**
	 * A table T is considered to be a link table if it has exactly two
	 * foreign key constraints, and the constraints reference other
	 * tables (not T), and the constraints cover all columns of T,
	 * and there are no foreign keys from other tables pointing to this table
	 */
	public boolean isLinkTable(RelationName tableName) {
		List<Join> foreignKeys = schema.foreignKeys(tableName, DatabaseSchemaInspector.KEYS_IMPORTED);
		if (foreignKeys.size() != 2) return false;
		
		List<Join> exportedKeys = schema.foreignKeys(tableName, DatabaseSchemaInspector.KEYS_EXPORTED);
		if (!exportedKeys.isEmpty()) return false;
		
		List<Attribute> columns = schema.listColumns(tableName);
		Iterator<Join> it = foreignKeys.iterator();
		while (it.hasNext()) {
			Join fk = it.next();
			if (fk.isSameTable()) return false;
			columns.removeAll(fk.attributes1());
		}
		return columns.isEmpty();
	}

	private boolean isInForeignKey(Attribute column, List<Join> foreignKeys) {
		for (Join fk: foreignKeys) {
			if (fk.containsColumn(column)) return true;
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
		Resource r = ontologyResource();
		r.addProperty(RDF.type, OWL.Ontology);
		r.addProperty(OWL.imports, this.vocabModel.getResource(dropTrailingHash(DC.getURI())));
		r.addProperty(DC.creator, CREATOR);
	}

	private void createVocabularyClass(RelationName tableName) {
		Resource r = classResource(tableName);
		r.addProperty(RDF.type, RDFS.Class);
		r.addProperty(RDF.type, OWL.Class);
		r.addProperty(RDFS.label, tableName.qualifiedName());
		r.addProperty(RDFS.isDefinedBy, ontologyResource());
	}

	private void createDatatypeProperty(Attribute column, RDFDatatype datatype) {
		Resource r = propertyResource(column);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, toUniqueString(column));
		r.addProperty(RDFS.domain, classResource(column.relationName()));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(Join join) {
		Resource r = propertyResource(join.attributes1());
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, toUniqueString(join.attributes1()));
		r.addProperty(RDFS.domain, classResource(schema.getCorrectCapitalization(join.table1())));
		r.addProperty(RDFS.range, classResource(schema.getCorrectCapitalization(join.table2())));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
	}
	
	private void createLinkProperty(RelationName linkTableName, RelationName fromTable, RelationName toTable) {
		String propertyURI = this.vocabNamespaceURI + linkTableName;
		Resource r = this.vocabModel.createResource(propertyURI);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, linkTableName.qualifiedName());
		r.addProperty(RDFS.domain, classResource(fromTable));
		r.addProperty(RDFS.range, classResource(toTable));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());
	}
	
	private String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}
