package de.fuberlin.wiwiss.d2rq.mapgen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Generates a D2RQ mapping by introspecting a database schema.
 * Result is available as a high-quality N3 serialization, or
 * as a parsed model.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MappingGenerator.java,v 1.16 2006/09/15 17:53:37 cyganiak Exp $
 */
public class MappingGenerator {
	private final static String CREATOR = "D2RQ Mapping Generator";
	private String jdbcURL;
	private String mapNamespaceURI;
	private String instanceNamespaceURI;
	private String vocabNamespaceURI;
	private String driverClass = null;
	private String databaseUser = null;
	private String databasePassword = null;
	private PrintWriter out = null;
	private Model vocabModel = ModelFactory.createDefaultModel();
	private DatabaseSchemaInspector schema = null;
	private String databaseType = null;
	private Map linkTables = new HashMap(); // name of n:m link table => name of n table
	private boolean finished = false;

	public MappingGenerator(String jdbcURL) {
		this.jdbcURL = jdbcURL;
		this.mapNamespaceURI = "#";
		this.instanceNamespaceURI = this.jdbcURL + "#";
		this.vocabNamespaceURI = this.jdbcURL + "/vocab#";
		this.driverClass = Database.guessJDBCDriverClass(this.jdbcURL);
	}

	private void connectToDatabase() {
		// TODO What URI to use here?
		Database database = new Database(ResourceFactory.createResource());
		database.setJDBCDSN(this.jdbcURL);
		database.setJDBCDriver(this.driverClass);
		database.setUsername(this.databaseUser);
		database.setPassword(this.databasePassword);
		this.schema = database.connectedDB().schemaInspector();
		this.databaseType = database.connectedDB().dbType();
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

	public void setDatabaseUser(String user) {
		this.databaseUser = user;
	}

	public void setDatabasePassword(String password) {
		this.databasePassword = password;
	}

	public void setJDBCDriverClass(String driverClassName) {
		this.driverClass = driverClassName;
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
	
	public void writeMapping(Writer out) {
		this.out = new PrintWriter(out);
		if (this.schema == null) {
			connectToDatabase();
		}
		run();
		this.out.flush();
		this.out = null;
		this.finished = true;
	}

	public Model vocabularyModel() {
		if (!this.finished) {
			StringWriter w = new StringWriter();
			writeMapping(w);
		}
		return this.vocabModel;
	}
	
	public Model mappingModel(String baseURI) {
		StringWriter w = new StringWriter();
		writeMapping(w);
		String mappingAsN3 = w.toString();
		Model result = ModelFactory.createDefaultModel();
		result.read(new StringReader(mappingAsN3), baseURI, "N3");
		return result;
	}
	
	private void run() {
		this.out.println("@prefix map: <" + this.mapNamespaceURI + "> .");
		this.out.println("@prefix db: <" + this.instanceNamespaceURI + "> .");
		this.out.println("@prefix vocab: <" + this.vocabNamespaceURI + "> .");
		this.out.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		this.out.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		this.out.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		this.out.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		this.out.println();
		writeDatabase();
		initVocabularyModel();
		identifyLinkTables();
		Iterator it = schema.listTableNames().iterator();
		while (it.hasNext()) {
			RelationName tableName = (RelationName) it.next();
			if (!this.schema.isLinkTable(tableName)) {
				writeTable(tableName);
			}
		}
	}
	
	private void writeDatabase() {
		this.out.println(databaseName() + " a d2rq:Database;");
		this.out.println("\td2rq:jdbcDriver \"" + this.driverClass + "\";");
		this.out.println("\td2rq:jdbcDSN \"" + this.jdbcURL + "\";");
		if (this.databaseUser != null) {
			this.out.println("\td2rq:username \"" + this.databaseUser + "\";");
		}
		if (this.databasePassword != null) {
			this.out.println("\td2rq:password \"" + this.databasePassword + "\";");
		}
		this.out.println("\t.");
		this.out.println();
	}
	
	public void writeTable(RelationName tableName) {
		this.out.println("# Table " + tableName);
		this.out.println(classMapName(tableName) + " a d2rq:ClassMap;");
		this.out.println("\td2rq:dataStorage " + databaseName() + ";");
		if (!hasPrimaryKey(tableName)) {
			this.out.println("\t# Sorry, I don't know which columns to put into the uriPattern");
			this.out.println("\t# because the table doesn't have a primary key");
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern(tableName) + "\";");
		this.out.println("\td2rq:class " + vocabularyTermQName(tableName) + ";");
		this.out.println("\t.");
		writeLabelBridge(tableName);
		List foreignKeys = this.schema.foreignKeyColumns(tableName);
		Iterator it = this.schema.listColumns(tableName).iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			writeColumn(column, foreignKeys);
		}
		it = this.linkTables.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			if (entry.getValue().equals(tableName)) {
				writeLink((RelationName) entry.getKey());
			}
		}
		this.out.println();
		createVocabularyClass(tableName);
	}

	public void writeLabelBridge(RelationName tableName) {
		this.out.println(propertyBridgeName(tableName.qualifiedName()) + "__label a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(tableName) + ";");
		this.out.println("\td2rq:property rdfs:label;");
		this.out.println("\td2rq:pattern \"" + labelPattern(tableName) + "\";");
		this.out.println("\t.");
	}
	
	public void writeColumn(Attribute column, List foreignKeys) {
		this.out.println(propertyBridgeName(toRelationName(column)) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(column.relationName()) + ";");
		this.out.println("\td2rq:property " + vocabularyTermQName(column) + ";");
		Attribute foreignKeyColumn = null;
		Iterator it = foreignKeys.iterator();
		while (it.hasNext()) {
			Attribute[] linkedColumns = (Attribute[]) it.next();
			if (linkedColumns[0].equals(column)) {
				foreignKeyColumn = linkedColumns[1];
			}
		}
		if (foreignKeyColumn == null) {
			this.out.println("\td2rq:column \"" + column.qualifiedName() + "\";");
			int colType = this.schema.columnType(column);
			String xsd = DatabaseSchemaInspector.xsdTypeFor(colType);
			if (xsd != null && !"xsd:string".equals(xsd)) {
				// We use plain literals instead of xsd:strings, so skip
				// this if it's an xsd:string
				this.out.println("\td2rq:datatype " + xsd + ";");
			}
			writeColumnHacks(column, colType);
			if (xsd == null) {
				createDatatypeProperty(column, null);
			} else {
				String datatypeURI = xsd.replaceAll("xsd:", XSDDatatype.XSD + "#");
				createDatatypeProperty(column, TypeMapper.getInstance().getSafeTypeByName(datatypeURI));
			}
		} else {
			this.out.println("\td2rq:refersToClassMap " + classMapName(foreignKeyColumn.relationName()) + ";");
			Attribute joinColumn = foreignKeyColumn;
			// Same-table join? Then we need to set up an alias for the table and join to that
			if (column.relationName().equals(foreignKeyColumn.relationName())) {
				String aliasName = foreignKeyColumn.relationName().qualifiedName().replace('.', '_') + "__alias";
				this.out.println("\td2rq:alias \"" + column.relationName().qualifiedName() + " AS " + aliasName + "\";");
				joinColumn = new Attribute(null, aliasName, foreignKeyColumn.attributeName());
			}
			this.out.println("\td2rq:join \"" + column.qualifiedName() + " = " + joinColumn.qualifiedName() + "\";");
			createObjectProperty(column, foreignKeyColumn);
		}
		this.out.println("\t.");
	}

	// TODO Factor out into its own interface & classes for different RDBMS?
	public void writeColumnHacks(Attribute column, int colType) {
//		if (DatabaseSchemaInspector.isStringType(colType)) {
//			// Suppress empty strings ('')
//			out.println("\td2rq:condition \"" + column.getQualifiedName() + " != ''\";");			
//		}
		if (this.databaseType == "MySQL" && DatabaseSchemaInspector.isDateType(colType)) {
			// Work around an issue with the MySQL driver where SELECTing a date/time
			// column containing '0000-00-00 ...' causes an SQLException
			this.out.println("\td2rq:condition \"" + column.qualifiedName() + " != '0000'\";");
		}
	}
	
	private void writeLink(RelationName linkTableName) {
		List foreignKeys = this.schema.foreignKeyColumns(linkTableName);
		Attribute firstLocalColumn = ((Attribute[]) foreignKeys.get(0))[0];
		Attribute firstForeignColumn = ((Attribute[]) foreignKeys.get(0))[1];
		Attribute secondLocalColumn = ((Attribute[]) foreignKeys.get(1))[0];
		Attribute secondForeignColumn = ((Attribute[]) foreignKeys.get(1))[1];
		this.out.println("# n:m table " + linkTableName);
		this.out.println(propertyBridgeName(linkTableName.qualifiedName()) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(firstForeignColumn.relationName()) + ";");
		this.out.println("\td2rq:property " + vocabularyTermQName(linkTableName) + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapName(secondForeignColumn.relationName()) + ";");
		this.out.println("\td2rq:join \"" + firstForeignColumn.qualifiedName() + " = " + firstLocalColumn.qualifiedName() + "\";");
		this.out.println("\td2rq:join \"" + secondLocalColumn.qualifiedName() + " = " + secondForeignColumn.qualifiedName() + "\";");
		this.out.println("\t.");
		createLinkProperty(linkTableName, firstForeignColumn.relationName(), secondForeignColumn.relationName());
	}

	private String databaseName() {
		return "map:database";
	}
	
	private String classMapName(RelationName tableName) {
		return "map:" + qNameEscape(tableName.qualifiedName());
	}
	
	private String propertyBridgeName(String relationName) {
		return "map:" + qNameEscape(relationName);
	}
	
	private String qNameEscape(String s) {
		// The Jena N3 parser can't deal with dots in QNames
		return s.replace('.', '_');
	}

	private String vocabularyTermQName(RelationName table) {
		return "vocab:" + qNameEscape(table.qualifiedName());
	}

	private String vocabularyTermQName(Attribute attribute) {
		return "vocab:" + qNameEscape(toRelationName(attribute));
	}

	private boolean hasPrimaryKey(RelationName tableName) {
		return !this.schema.primaryKeyColumns(tableName).isEmpty();
	}
	
	private String uriPattern(RelationName tableName) {
		String result = this.instanceNamespaceURI + tableName.qualifiedName();
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			result += "/@@" + column.qualifiedName() + "@@";
		}
		return result;
	}
	
	private String labelPattern(RelationName tableName) {
		String result = tableName + " #";
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			result += "@@" + column.qualifiedName() + "@@";
			if (it.hasNext()) {
				result += "/";
			}
		}
		return result;
	}
	
	private String toRelationName(Attribute column) {
		return column.tableName() + "_" + column.attributeName();
	}

	private void identifyLinkTables() {
		Iterator it = this.schema.listTableNames().iterator();
		while (it.hasNext()) {
			RelationName tableName = (RelationName) it.next();
			if (!this.schema.isLinkTable(tableName)) {
				continue;
			}
			Attribute[] firstForeignKey = (Attribute[]) this.schema.foreignKeyColumns(tableName).get(0);
			this.linkTables.put(tableName, firstForeignKey[1].relationName().qualifiedName());
		}
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
		r.addProperty(RDFS.label, tableName);
		r.addProperty(RDFS.isDefinedBy, ontologyResource());
	}

	private void initProperty(Resource r, Attribute column) {
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, toRelationName(column));
		r.addProperty(RDFS.domain, classResource(column.relationName()));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
	}
	
	private void createDatatypeProperty(Attribute column, RDFDatatype datatype) {
		Resource r = propertyResource(column);
		initProperty(r, column);
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(Attribute subjectColumn, Attribute objectColumn) {
		Resource r = propertyResource(subjectColumn);
		initProperty(r, subjectColumn);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.range, classResource(objectColumn.relationName()));
	}

	private void createLinkProperty(RelationName linkTableName, RelationName fromTable, RelationName toTable) {
		String propertyURI = this.vocabNamespaceURI + linkTableName;
		Resource r = this.vocabModel.createResource(propertyURI);
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.label, linkTableName);
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
	
	private String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}