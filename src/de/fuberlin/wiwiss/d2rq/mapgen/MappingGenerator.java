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
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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
 * TODO: This does some progress logging to stdout if writing to
 *       a file. This should perhaps be done via log.info(...) with
 *       an appropriately configured log writer.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MappingGenerator {
	private final static String CREATOR = "D2RQ Mapping Generator";
	private final static OutputStream DUMMY_STREAM = 
			new OutputStream() { public void write(int b) {}};

	protected final ConnectedDB database;
	private final DatabaseSchemaInspector schema;
	private String mapNamespaceURI;
	protected String instanceNamespaceURI;
	protected String vocabNamespaceURI;
	private String driverClass = null;
	private Filter filter = Filter.ALL;
	private PrintWriter out = null;
	private PrintWriter err = null;
	private PrintWriter progress = null;
	private Model vocabModel = ModelFactory.createDefaultModel();
	// name of n:m link table => name of n table
	private Map<RelationName,RelationName> linkTables = new HashMap<RelationName,RelationName>();
	private boolean finished = false;
	private boolean generateClasses = true;
	private boolean generateLabelBridges = true;
	private boolean generateDefinitionLabels = true;
	private boolean handleLinkTables = true;
	private boolean serveVocabulary = true;
	private URI startupSQLScript;
	
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

	public void writeMapping(OutputStream out, OutputStream err, OutputStream progress) {
		try {
			Writer w = new OutputStreamWriter(out, "UTF-8");
			Writer e = (err != null) ? new OutputStreamWriter(err, "UTF-8") : null;
			Writer p = (progress != null) ? new OutputStreamWriter(progress, "UTF-8") : null;
			writeMapping(w, e, p);
			w.flush();			
			if (e != null) e.flush();
			if (p != null) p.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Writes the D2RQ mapping to a writer. Note that the calling code is
	 * responsible for closing the writers after use.
	 * 
	 * @param out Stream for writing data to the file or console
	 * @param err Stream for warnings generated during the mapping process; may be <code>null</code>
	 * @param progress Stream for reporting progress; may be <code>null</code>
	 */
	public void writeMapping(Writer out, Writer err, Writer progress) {
		this.out = new PrintWriter(out);
		this.err = (err != null) ? new PrintWriter(err) : new PrintWriter(DUMMY_STREAM);
		this.progress = (progress != null) ? new PrintWriter(progress) : new PrintWriter(DUMMY_STREAM);
		run();
		this.progress.println("Done!");
		this.out.flush();
		this.err.flush();
		this.progress.flush();
		this.out = null;
		this.finished = true;
	}

	public Model vocabularyModel(OutputStream err, OutputStream progress) {
		if (!this.finished) {
			writeMapping(DUMMY_STREAM, err, progress);
		}
		return this.vocabModel;
	}
	
	/**
	 * Returns an in-memory Jena model containing the D2RQ mapping.
	 * 
	 * @param baseURI Base URI for resolving relative URIs in the mapping, e.g., map namespace
	 * @param err Stream for warnings generated during the mapping process; may be <code>null</code>
	 * @return In-memory Jena model containing the D2RQ mapping
	 */
	public Model mappingModel(String baseURI, OutputStream err, OutputStream progress) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeMapping(out, err, progress);
		try {
			String mappingAsTurtle = out.toString("UTF-8");
			Model result = ModelFactory.createDefaultModel();
			result.read(new StringReader(mappingAsTurtle), baseURI, "TURTLE");
			return result;
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("UTF-8 is always supported");
		}
	}
	
	private void run() {
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
		if (handleLinkTables) {
			identifyLinkTables();
		}
		for (RelationName tableName: schema.listTableNames(filter.getSingleSchema())) {
			if (!filter.matches(tableName)) {
				progress.println("Skipping table " + tableName);
				continue;
			}
			if (!handleLinkTables || !schema.isLinkTable(tableName)) {
				writeTable(tableName);
			}
		}
	}
	
	private void writeConfiguration() {
		progress.println("Generating d2rq:Configuration instance");
		this.out.println(configName() + " a d2rq:Configuration;");
		this.out.println("\td2rq:serveVocabulary false.");
		this.out.println();
	}

	private String configName() {
		return "map:Configuration";
	}

	private void writeDatabase() {
		progress.println("Generating d2rq:Database instance");
		this.out.println(databaseName() + " a d2rq:Database;");
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
		progress.println("Generating d2rq:ClassMap instance for table " + tableName.qualifiedName()) ;
		this.out.println("# Table " + tableName);
		this.out.println(classMapName(tableName) + " a d2rq:ClassMap;");
		this.out.println("\td2rq:dataStorage " + databaseName() + ";");
		if (!hasPrimaryKey(tableName)) {
			writeWarning(new String[]{
					"Sorry, I don't know which columns to put into the uriPattern",
					"    for \"" + tableName + "\" because the table doesn't have a primary key.", 
					"    Please specify it manually."
				}, "\t");
		}
				
		this.out.println("\td2rq:uriPattern \"" + uriPattern(tableName) + "\";");
		if (generateClasses) {
			this.out.println("\td2rq:class " + vocabularyTermQName(tableName) + ";");
			if (generateDefinitionLabels) {
				this.out.println("\td2rq:classDefinitionLabel \"" + tableName + "\";");
			}
		}
		this.out.println("\t.");
		if (generateLabelBridges && hasPrimaryKey(tableName)) {
			writeLabelBridge(tableName);
		}
		List<Join> foreignKeys = schema.foreignKeys(tableName, DatabaseSchemaInspector.KEYS_IMPORTED);
		for (Attribute column: schema.listColumns(tableName)) {
			if (!filter.matches(column)) {
				progress.println("Skipping column " + column);
				continue;
			}
			if (isInForeignKey(column, foreignKeys)) continue;
			if (schema.columnType(column) == null) {
				writeWarning(new String[]{
						"Skipping column: " + column,
						"    Its datatype " + schema.columnType(column) + " is unknown to D2RQ.",
						"    You can override the column's datatype using d2rq:xxxColumn and add a property bridge.",
					}, "");
				continue;
			}
			if (schema.columnType(column).isUnsupported()) {
				writeWarning(new String[]{
						"Skipping column: " + column,
						"    Its datatype " + schema.columnType(column) + " cannot be mapped to RDF"
					}, "");
				continue;
			}
			writeColumn(column);
		}
		for (Join fk: foreignKeys) {
			if (!filter.matches(fk.table1()) || !filter.matches(fk.table2()) || 
					!filter.matchesAll(fk.attributes1()) || !filter.matchesAll(fk.attributes2())) {
				progress.println("Skipping foreign key: " + fk);
				continue;
			}
			writeForeignKey(fk);
		}
		if (handleLinkTables) {
			for (RelationName linkTable: linkTables.keySet()) {
				if (linkTables.get(linkTable).equals(tableName)) {
					List<Join> keys = schema.foreignKeys(linkTable, DatabaseSchemaInspector.KEYS_IMPORTED);
					Join join1 = keys.get(0);
					Join join2 = keys.get(1);
					if (!filter.matches(join1.table1()) || !filter.matches(join1.table2()) || 
							!filter.matchesAll(join1.attributes1()) || !filter.matchesAll(join1.attributes2()) ||
							!filter.matches(join2.table1()) || !filter.matches(join2.table2()) || 
							!filter.matchesAll(join2.attributes1()) || !filter.matchesAll(join2.attributes2())) {
						progress.println("Skipping link table " + linkTable);
						continue;
					}
					writeLinkTable(linkTable, keys);
				}
			}
		}
		this.out.println();
		createVocabularyClass(tableName);
		//
		// Let a tail see that we're working and ensure that every table that did
		// NOT generate an exception is actually saved...
		//
		this.out.flush();
	}

	public void writeLabelBridge(RelationName tableName) {
		this.out.println(propertyBridgeName(tableName.qualifiedName(), "__label") + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(tableName) + ";");
		this.out.println("\td2rq:property rdfs:label;");
		this.out.println("\td2rq:pattern \"" + labelPattern(tableName) + "\";");
		this.out.println("\t.");
	}
	
	public void writeColumn(Attribute column) {
		this.out.println(propertyBridgeName(toRelationName(column)) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(column.relationName()) + ";");
		this.out.println("\td2rq:property " + vocabularyTermQName(column) + ";");
		if (generateDefinitionLabels) {
			this.out.println("\td2rq:propertyDefinitionLabel \"" + toRelationLabel(column) + "\";");
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
		writeColumnHacks(column, colType);
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
		this.out.println(propertyBridgeName(toRelationName(primaryColumns)) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(primaryTable) + ";");
		this.out.println("\td2rq:property " + vocabularyTermQName(primaryColumns) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapName(foreignTable) + ";");
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

	// TODO Factor out into its own interface & classes for different RDBMS?
	public void writeColumnHacks(Attribute column, DataType colType) {
//		if (DatabaseSchemaInspector.isStringType(colType)) {
//			// Suppress empty strings ('')
//			out.println("\td2rq:condition \"" + column.getQualifiedName() + " != ''\";");			
//		}
	}
	
	private void writeLinkTable(RelationName linkTableName, List<Join> foreignKeys) {
		Join join1 = (Join) foreignKeys.get(0);
		Join join2 = (Join) foreignKeys.get(1);
		RelationName table1 = this.schema.getCorrectCapitalization(join1.table2());
		RelationName table2 = this.schema.getCorrectCapitalization(join2.table2());
		boolean isSelfJoin = table1.equals(table2);
		this.out.println("# n:m table " + linkTableName + (isSelfJoin ? " (self-join)" : ""));
		this.out.println(propertyBridgeName(linkTableName.qualifiedName()) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(table1) + ";");
		this.out.println("\td2rq:property " + vocabularyTermQName(linkTableName) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapName(table2) + ";");
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
		createLinkProperty(linkTableName, table1, table2);
	}

	private void writeWarning(String[] warning, String indent) {
		for (int i=0; i<warning.length; i++)
			this.out.println(indent + "# " + warning[i]);
		
		if (this.err != null) {
			for (int i=0; i<warning.length; i++)
				this.err.println(warning[i]);
		}
	}
	
	private String databaseName() {
		return "map:database";
	}
	
	// A very conservative pattern for the local part of a QName.
	// If a URI doesn't match, better don't QName it.
	protected final static Pattern simpleXMLName = 
			Pattern.compile("[a-zA-Z_][a-zA-Z0-9_-]*");
	protected String toPrefixedURI(String namespaceURI, String prefix, String s) {
		if (simpleXMLName.matcher(s).matches()) {
			return prefix + ":" + s;
		}
		try {
			return "<" + namespaceURI + URLEncoder.encode(s, "utf-8") + ">";
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException("Can't happen");
		}
	}

	private String classMapName(RelationName tableName) {
		return toPrefixedURI(mapNamespaceURI, "map", tableName.qualifiedName());
	}
	
	private String propertyBridgeName(String relationName) {
		return propertyBridgeName(relationName, "");
	}
	
	private String propertyBridgeName(String relationName, String suffix) {
		return toPrefixedURI(mapNamespaceURI, "map", relationName + suffix);
	}
	
	protected String vocabularyTermQName(RelationName table) {
		return toPrefixedURI(vocabNamespaceURI, "vocab", table.qualifiedName());
	}

	protected String vocabularyTermQName(Attribute attribute) {
		return toPrefixedURI(vocabNamespaceURI, "vocab", toRelationName(attribute));
	}

	protected String vocabularyTermQName(List<Attribute> attributes) {
		return toPrefixedURI(vocabNamespaceURI, "vocab", toRelationName(attributes));
	}

	protected List<Attribute> filteredPrimaryKeyColumns(RelationName tableName) {
		List<Attribute> columns = schema.primaryKeyColumns(tableName);
		if (filter.matchesAll(columns)) {
			return columns;
		}
		return Collections.<Attribute>emptyList();
	}
	
	private boolean hasPrimaryKey(RelationName tableName) {
		return !filteredPrimaryKeyColumns(tableName).isEmpty();
	}
	
	protected String uriPattern(RelationName tableName) {
		String result = this.instanceNamespaceURI + tableName.qualifiedName();
		for (Attribute column: filteredPrimaryKeyColumns(tableName)) {
			result += "/@@" + column.qualifiedName();
			if (!schema.columnType(column).isIRISafe()) {
				result += "|urlify";
			}
			result += "@@";
		}
		return result;
	}
	
	private String labelPattern(RelationName tableName) {
		String result = tableName + " #";
		Iterator<Attribute> it = filteredPrimaryKeyColumns(tableName).iterator();
		while (it.hasNext()) {
			result += "@@" + it.next().qualifiedName() + "@@";
			if (it.hasNext()) {
				result += "/";
			}
		}
		return result;
	}
	

	private String toRelationName(Attribute column) {
		return column.tableName() + "_" + column.attributeName();
	}

	private String toRelationName(List<Attribute> columns) {
		StringBuffer result = new StringBuffer();
		result.append((columns.get(0)).tableName());
		for (Attribute column: columns) {
			result.append("_" + column.attributeName());
		}
		return result.toString();
	}
	
	private String toRelationLabel(Attribute column) {
		return column.tableName() + " " + column.attributeName();
	}
	
	private void identifyLinkTables() {
		progress.println("Identifying link tables");
		for (RelationName tableName: schema.listTableNames(filter.getSingleSchema())) {
			if (!this.schema.isLinkTable(tableName)) {
				continue;
			}
			Join firstForeignKey = (Join) this.schema.foreignKeys(tableName, DatabaseSchemaInspector.KEYS_IMPORTED).get(0);
			this.linkTables.put(tableName, firstForeignKey.table2());
		}
		progress.println("Found " + String.valueOf(this.linkTables.size()) + " link tables") ;
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
		r.addProperty(RDFS.label, toRelationName(column));
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
		r.addProperty(RDFS.label, toRelationName(join.attributes1()));
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
	
	private Resource ontologyResource() {
		String ontologyURI = dropTrailingHash(vocabNamespaceURI);
		return this.vocabModel.createResource(ontologyURI);
	}
	
	private Resource classResource(RelationName tableName) {
		String classURI = this.vocabNamespaceURI + tableName.qualifiedName().replace('.', '_');
		return this.vocabModel.createResource(classURI);		
	}

	private Resource propertyResource(Attribute column) {
		String propertyURI = this.vocabNamespaceURI + toRelationName(column);
		return this.vocabModel.createResource(propertyURI);
	}
	
	private Resource propertyResource(List<Attribute> columns) {
		String propertyURI = this.vocabNamespaceURI + toRelationName(columns);
		return this.vocabModel.createResource(propertyURI);
	}
	
	private String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}
