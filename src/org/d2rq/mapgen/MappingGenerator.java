package org.d2rq.mapgen;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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
import org.d2rq.lang.JoinSetParser;
import org.d2rq.lang.Microsyntax;
import org.d2rq.pp.PrettyPrinter;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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
	private final static OutputStream DUMMY_STREAM = 
			new OutputStream() { public void write(int b) {}};

	protected final SQLConnection sqlConnection;
	private String mapNamespaceURI;
	protected String instanceNamespaceURI;
	private String vocabNamespaceURI;
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
				writeLinkTable(table);
			} else {
				writeTable(table);
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
		this.out.println("\td2rq:jdbcDriver \"" + sqlConnection.getJdbcDriverClass() + "\";");
		this.out.println("\td2rq:jdbcURL \"" + sqlConnection.getJdbcURL() + "\";");
		if (sqlConnection.getUsername() != null) {
			this.out.println("\td2rq:username \"" + sqlConnection.getUsername() + "\";");
		}
		if (sqlConnection.getPassword() != null) {
			this.out.println("\td2rq:password \"" + sqlConnection.getPassword() + "\";");
		}
		if (startupSQLScript != null) {
			out.println("\td2rq:startupSQLScript <" + startupSQLScript + ">;");
		}
		Properties props = sqlConnection.vendor().getDefaultConnectionProperties();
		for (Object property: props.keySet()) {
			String value = props.getProperty((String) property);
			this.out.println("\tjdbc:" + property + " \"" + value + "\";");
		}
		this.out.println("\t.");
		this.out.println();
		this.out.flush(); // Let a tail see that we're working
	}
	
	public void writeTable(TableDef table) {
		log.info("Generating d2rq:ClassMap instance for table " + Microsyntax.toString(table.getName()));
		this.out.println("# Table " + Microsyntax.toString(table.getName()));
		List<Identifier> identifierColumns = identifierColumns(table);
		boolean hasIdentifier = identifierColumns != null && !identifierColumns.isEmpty();
		this.out.println(classMapIRITurtle(table.getName()) + " a d2rq:ClassMap;");
		this.out.println("\td2rq:dataStorage " + databaseIRITurtle() + ";");
		if (hasIdentifier) {
			writeEntityIdentifier(table, identifierColumns);
		} else {
			writePseudoEntityIdentifier(table);
		}

		if (generateClasses) {
			this.out.println("\td2rq:class " + vocabularyIRITurtle(table.getName()) + ";");
			if (generateDefinitionLabels) {
				this.out.println("\td2rq:classDefinitionLabel \"" + Microsyntax.toString(table.getName()) + "\";");
			}
		}
		this.out.println("\t.");
		if (generateLabelBridges && hasIdentifier) {
			writeLabelBridge(table, identifierColumns);
		}
		Collection<ForeignKey> foreignKeys = table.getForeignKeys();
		for (Identifier column: filter(table, table.getColumnNames(), false, "property bridge")) {
			if (skipForeignKeyTargetColumns && isInForeignKey(column, table)) continue;
			writeColumn(table, column);
		}
		for (ForeignKey fk: foreignKeys) {
			if (!filter.matches(fk.getReferencedTable()) || 
					!filter.matchesAll(table.getName(), fk.getLocalColumns()) || 
					!filter.matchesAll(fk.getReferencedTable(), fk.getReferencedColumns())) {
				log.info("Skipping foreign key: " + fk);
				continue;
			}
			writeForeignKey(table, fk);
		}
		createVocabularyClass(table.getName());
		this.out.println();
		// Let a tail see that we're working and ensure that every table that did
		// NOT generate an exception is actually saved...
		this.out.flush();
	}

	protected void writeEntityIdentifier(TableDef table, 
			List<Identifier> identifierColumns) {
		String uriPattern = this.instanceNamespaceURI;
		if (table.getName().getSchema() != null) {
			uriPattern += IRIEncoder.encode(table.getName().getSchema().getName()) + "/";
		}
		uriPattern += IRIEncoder.encode(table.getName().getTable().getName());
		for (Identifier column: identifierColumns) {
			uriPattern += "/@@" + Microsyntax.toString(table.getName(), column);
			if (!table.getColumnDef(column).getDataType().isIRISafe()) {
				uriPattern += "|urlify";
			}
			uriPattern += "@@";
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern + "\";");
	}
	
	protected void writePseudoEntityIdentifier(TableDef table) {
		writeWarning(new String[]{
				"Sorry, I don't know which columns to put into the uriPattern",
				"    for \"" + Microsyntax.toString(table.getName()) + "\" because the table doesn't have a primary key.", 
				"    Please specify it manually."
			}, "\t");
		writeEntityIdentifier(table, Collections.<Identifier>emptyList());
	}
	
	public void writeLabelBridge(TableDef table, List<Identifier> labelColumns) {
		this.out.println(propertyBridgeIRITurtle(table.getName(), "label") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(table.getName()) + ";");
		this.out.println("\td2rq:property rdfs:label;");
		this.out.println("\td2rq:pattern \"" + labelPattern(table.getName(), 
				labelColumns) + "\";");
		this.out.println("\t.");
	}
	
	public void writeColumn(TableDef table, Identifier column) {
		this.out.println(propertyBridgeIRITurtle(table.getName(), column) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(table.getName()) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(table.getName(), column) + ";");
		if (generateDefinitionLabels) {
			this.out.println("\td2rq:propertyDefinitionLabel \"" + toLabel(table.getName(), column) + "\";");
		}
		this.out.println("\td2rq:column \"" + Microsyntax.toString(table.getName(), column) + "\";");
		DataType colType = table.getColumnDef(column).getDataType();
		String datatypeURI = colType.rdfType();
		if (datatypeURI != null && !XSD.xstring.getURI().equals(datatypeURI)) {
			// We use plain literals instead of xsd:strings, so skip
			// this if it's an xsd:string
			this.out.println("\td2rq:datatype " + PrettyPrinter.toString(Node.createURI(datatypeURI)) + ";");
		}
		if (colType.valueRegex() != null) {
			this.out.println("\td2rq:valueRegex \"" + colType.valueRegex() + "\";");
		}
		if (datatypeURI == null) {
			createDatatypeProperty(table.getName(), column, null);
		} else {
			createDatatypeProperty(table.getName(), column, TypeMapper.getInstance().getSafeTypeByName(datatypeURI));
		}
		this.out.println("\t.");
	}

	public void writeForeignKey(TableDef table, ForeignKey foreignKey) {
		Key foreignKeyColumns = foreignKey.getLocalColumns();
		TableName referencedTable = foreignKey.getReferencedTable();
		this.out.println(propertyBridgeIRITurtle(table.getName(), foreignKeyColumns, "ref") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(table.getName()) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(table.getName(), foreignKeyColumns) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapIRITurtle(referencedTable) + ";");
		TableName target = referencedTable;
		if (foreignKey.getReferencedTable().equals(table.getName())) {
			// Same-table join? Then we need to set up an alias for the table and join to that
			Identifier aliasName = Identifier.create(true, 
					Microsyntax.toString(referencedTable).replace('.', '_') + "__alias");
			this.out.println("\td2rq:alias \"" + Microsyntax.toString(referencedTable) + " AS " + aliasName.getName() + "\";");
			target = TableName.create(null, null, aliasName);
		}
		for (int i = 0; i < foreignKeyColumns.size(); i++) {
			Identifier column1 = foreignKeyColumns.get(i);
			Identifier column2 = foreignKey.getReferencedColumns().get(i);
			this.out.println("\td2rq:join \"" + Microsyntax.toString(table.getName(), column1) + " " + 
					JoinSetParser.Direction.RIGHT.operator + " " +
					Microsyntax.toString(target, column2) + "\";");
		}
		createObjectProperty(table.getName(), foreignKey);
		this.out.println("\t.");
	}

	private void writeLinkTable(TableDef linkTable) {
		TableName tableName = linkTable.getName();
		Collection<ForeignKey> keys = linkTable.getForeignKeys();
		Iterator<ForeignKey> it = keys.iterator();
		ForeignKey join1 = it.next();
		ForeignKey join2 = it.next();
		TableName referencedTable1 = join1.getReferencedTable();
		TableName referencedTable2 = join2.getReferencedTable();
		if (!filter.matches(referencedTable1) || 
				!filter.matchesAll(referencedTable1, join1.getLocalColumns()) || 
				!filter.matchesAll(referencedTable1, join1.getReferencedColumns()) ||
				!filter.matches(referencedTable2) || 
				!filter.matchesAll(referencedTable2, join2.getLocalColumns()) || 
				!filter.matchesAll(referencedTable2, join2.getReferencedColumns())) {
			log.info("Skipping link table " + Microsyntax.toString(linkTable.getName()));
			return;
		}
		log.info("Generating d2rq:PropertyBridge instance for table " + Microsyntax.toString(linkTable.getName()));
		boolean isSelfJoin = referencedTable1.equals(referencedTable2);
		this.out.println("# Table " + Microsyntax.toString(tableName) + (isSelfJoin ? " (n:m self-join)" : " (n:m)"));
		this.out.println(propertyBridgeIRITurtle(tableName, "link") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapIRITurtle(referencedTable1) + ";");
		this.out.println("\td2rq:property " + vocabularyIRITurtle(tableName) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapIRITurtle(referencedTable2) + ";");
		TableName targetTable = referencedTable2;
		if (isSelfJoin) {
			Identifier aliasName = Identifier.create(true, 
					referencedTable2.getTable().getName() + "_" + tableName.getTable().getName() + "__alias");
			targetTable = TableName.create(null, null, aliasName);
			this.out.println("\td2rq:alias \"" + Microsyntax.toString(referencedTable2) + 
					" AS " + aliasName.getName() + "\";");
		}
		for (int i = 0; i < join1.getLocalColumns().size(); i++) {
			Identifier column = join1.getLocalColumns().get(i); 
			Identifier otherColumn = join1.getReferencedColumns().get(i);
			this.out.println("\td2rq:join \"" + Microsyntax.toString(linkTable.getName(), column) + " " + 
					JoinSetParser.Direction.RIGHT + " " + 
					Microsyntax.toString(referencedTable1, otherColumn) + "\";");
		}
		for (int i = 0; i < join2.getLocalColumns().size(); i++) {
			Identifier column = join2.getLocalColumns().get(i); 
			Identifier otherColumn = join2.getReferencedColumns().get(i);
			this.out.println("\td2rq:join \"" + Microsyntax.toString(linkTable.getName(), column) + " " + 
					JoinSetParser.Direction.RIGHT + " " + 
					Microsyntax.toString(targetTable, otherColumn) + "\";");
		}
		this.out.println("\t.");
		this.out.println();
		createLinkProperty(tableName, referencedTable1, referencedTable2);
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
	
	private String classMapIRITurtle(TableName tableName) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName));
	}

	private String propertyBridgeIRITurtle(TableName tableName, String suffix) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName) + "__" + suffix);
	}

	private String propertyBridgeIRITurtle(TableName tableName, Identifier column) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName, column));
	}

	private String propertyBridgeIRITurtle(TableName tableName, Key columns, String suffix) {
		return toTurtleIRI(mapNamespaceURI, "map", toUniqueString(tableName, columns) + "__" + suffix);
	}

	protected String vocabularyIRITurtle(TableName tableName) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(tableName));
	}

	protected String vocabularyIRITurtle(TableName tableName, Identifier column) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(tableName, column));
	}

	protected String vocabularyIRITurtle(TableName tableName, Key columns) {
		return toTurtleIRI(vocabNamespaceURI, "vocab", toUniqueString(tableName, columns));
	}
	
	private Resource ontologyResource() {
		String ontologyURI = dropTrailingHash(vocabNamespaceURI);
		return this.vocabModel.createResource(ontologyURI);
	}
	
	private Resource classResource(TableName tableName) {
		String classURI = this.vocabNamespaceURI + toUniqueString(tableName);
		return this.vocabModel.createResource(classURI);		
	}

	private Resource propertyResource(TableName tableName, Identifier column) {
		String propertyURI = this.vocabNamespaceURI + toUniqueString(tableName, column);
		return this.vocabModel.createResource(propertyURI);
	}
	
	private Resource propertyResource(TableName tableName, Key columns) {
		String propertyURI = this.vocabNamespaceURI + toUniqueString(tableName, columns);
		return this.vocabModel.createResource(propertyURI);
	}

	private String toLabel(TableName tableName, Identifier column) {
		return tableName.getTable().getName() + " " + column.getName();
	}

	private String labelPattern(TableName tableName, List<Identifier> labelColumns) {
		String result = tableName.getTable().getName() + " #";
		Iterator<Identifier> it = labelColumns.iterator();
		while (it.hasNext()) {
			result += "@@" + Microsyntax.toString(tableName, it.next()) + "@@";
			if (it.hasNext()) {
				result += "/";
			}
		}
		return result;
	}

	private List<Identifier> identifierColumns(TableDef table) {
		if (table.getPrimaryKey() == null) return null;
		List<Identifier> columns = table.getPrimaryKey().getColumns();
		if (filter.matchesAll(table.getName(), columns)) {
			return filter(table, columns, true, "identifier column");
		}
		return null;
	}
	
	protected List<Identifier> filter(TableDef table, List<Identifier> columns, boolean requireDistinct, String reason) {
		List<Identifier> result = new ArrayList<Identifier>(columns.size());
		for (Identifier column: columns) {
			if (!filter.matches(table.getName(), column)) {
				log.info("Skipping filtered column " + column + " as " + reason);
				continue;
			}
			DataType type = table.getColumnDef(column).getDataType();
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
						"    Its datatype " + type + " cannot be mapped to RDF."
					}, "");
				continue;
			}
			if (requireDistinct && !type.supportsDistinct()) {
				writeWarning(new String[]{
						"Skipping column " + column + " as " + reason + ".",
						"    Its datatype " + type + " does not support DISTINCT."
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
	public boolean isLinkTable(TableDef table) {
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
		Resource r = ontologyResource();
		r.addProperty(RDF.type, OWL.Ontology);
		r.addProperty(OWL.imports, this.vocabModel.getResource(dropTrailingHash(DC.getURI())));
		r.addProperty(DC.creator, CREATOR);
	}

	private void createVocabularyClass(TableName tableName) {
		Resource r = classResource(tableName);
		r.addProperty(RDF.type, RDFS.Class);
		r.addProperty(RDF.type, OWL.Class);
		r.addProperty(RDFS.label, Microsyntax.toString(tableName));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());
	}

	private void createDatatypeProperty(TableName tableName, Identifier column, RDFDatatype datatype) {
		Resource r = propertyResource(tableName, column);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, toUniqueString(tableName, column));
		r.addProperty(RDFS.domain, classResource(tableName));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(TableName tableName, ForeignKey fk) {
		Resource r = propertyResource(tableName, fk.getLocalColumns());
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, toUniqueString(tableName, fk.getLocalColumns()));
		r.addProperty(RDFS.domain, classResource(tableName));
		r.addProperty(RDFS.range, classResource(fk.getReferencedTable()));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
	}
	
	private void createLinkProperty(TableName linkTableName, TableName fromTable, TableName toTable) {
		String propertyURI = this.vocabNamespaceURI + IRIEncoder.encode(linkTableName.getTable().getName());
		Resource r = this.vocabModel.createResource(propertyURI);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, Microsyntax.toString(linkTableName));
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
