package de.fuberlin.wiwiss.d2rq.mapgen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
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
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.DatabaseSchemaInspector;

public class MappingGenerator {
	private final static String CREATOR = "D2RQ Mapping Generator";
	private String jdbcURL;
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
		this.instanceNamespaceURI = this.jdbcURL + "#";
		this.vocabNamespaceURI = this.jdbcURL + "/vocab#";
	}

	private void connectToDatabase() {
		Database db = new Database(null, this.jdbcURL, this.driverClass,
				this.databaseUser, this.databasePassword, Collections.EMPTY_MAP);
		this.schema = new DatabaseSchemaInspector(db.getConnnection());
		if (this.driverClass == null) {
			this.driverClass = db.getJdbcDriver();
		}
		this.databaseType = db.getType();
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
		this.out.println("@prefix : <#> .");
		this.out.println("@prefix db: <" + this.instanceNamespaceURI + "> .");
		this.out.println("@prefix vocab: <" + this.vocabNamespaceURI + "> .");
		this.out.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		this.out.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		this.out.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		this.out.println();
		writeDatabase();
		initVocabularyModel();
		identifyLinkTables();
		Iterator it = schema.listTableNames().iterator();
		while (it.hasNext()) {
			String tableName = (String) it.next();
			if (!this.schema.isLinkTable(tableName)) {
				writeTable(tableName);
			}
		}
	}
	
	private void writeDatabase() {
		this.out.println(":database a d2rq:Database;");
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
	
	public void writeTable(String tableName) {
		this.out.println("# Table " + tableName);
		this.out.println(classMapName(tableName) + " a d2rq:ClassMap;");
		this.out.println("\td2rq:dataStorage :database;");
		if (!hasPrimaryKey(tableName)) {
			this.out.println("\t# Sorry, I don't know which columns to put into the uriPattern");
			this.out.println("\t# because the table doesn't have a primary key");
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern(tableName) + "\";");
		this.out.println("\td2rq:class vocab:" + tableName + ";");
		this.out.println("\t.");
		List foreignKeys = this.schema.foreignKeyColumns(tableName);
		Iterator it = this.schema.listColumns(tableName).iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			writeColumn(column, tableName, foreignKeys);
		}
		it = this.linkTables.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			if (entry.getValue().equals(tableName)) {
				writeLink((String) entry.getKey());
			}
		}
		this.out.println();
		createVocabularyClass(tableName);
	}
	
	public void writeColumn(Column column, String tableName, List foreignKeys) {
		this.out.println(":propertyBridge_" + name(column) + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(tableName) + ";");
		this.out.println("\td2rq:property vocab:" + name(column) + ";");
		Column foreignKeyColumn = null;
		Iterator it = foreignKeys.iterator();
		while (it.hasNext()) {
			Column[] linkedColumns = (Column[]) it.next();
			if (linkedColumns[0].equals(column)) {
				foreignKeyColumn = linkedColumns[1];
			}
		}
		if (foreignKeyColumn == null) {
			this.out.println("\td2rq:column \"" + column.getQualifiedName() + "\";");
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
			this.out.println("\td2rq:refersToClassMap " + classMapName(foreignKeyColumn.getTableName()) + ";");
			Column joinColumn = foreignKeyColumn;
			// Same-table join? Then we need to set up an alias for the table and join to that
			if (column.getTableName().equals(foreignKeyColumn.getTableName())) {
				String aliasedTableName = foreignKeyColumn.getTableName() + "__alias";
				this.out.println("\td2rq:alias \"" + column.getTableName() + " AS " + aliasedTableName + "\";");
				joinColumn = new Column(aliasedTableName, foreignKeyColumn.getColumnName());
			}
			this.out.println("\td2rq:join \"" + column.getQualifiedName() + " = " + joinColumn.getQualifiedName() + "\";");
			createObjectProperty(column, foreignKeyColumn);
		}
		this.out.println("\t.");
	}

	// TODO Factor out into its own interface & classes for different RDBMS?
	public void writeColumnHacks(Column column, int colType) {
//		if (DatabaseSchemaInspector.isStringType(colType)) {
//			// Suppress empty strings ('')
//			out.println("\td2rq:condition \"" + column.getQualifiedName() + " != ''\";");			
//		}
		if (this.databaseType == "MySQL" && DatabaseSchemaInspector.isDateType(colType)) {
			// Work around an issue with the MySQL driver where SELECTing a date/time
			// column containing '0000-00-00 ...' causes an SQLException
			this.out.println("\td2rq:condition \"" + column.getQualifiedName() + " != '0000'\";");
		}
	}
	
	private void writeLink(String linkTableName) {
		List foreignKeys = this.schema.foreignKeyColumns(linkTableName);
		Column firstLocalColumn = ((Column[]) foreignKeys.get(0))[0];
		Column firstForeignColumn = ((Column[]) foreignKeys.get(0))[1];
		Column secondLocalColumn = ((Column[]) foreignKeys.get(1))[0];
		Column secondForeignColumn = ((Column[]) foreignKeys.get(1))[1];
		this.out.println("# n:m table " + linkTableName);
		this.out.println(":propertyBridge_" + linkTableName + " a d2rq:PropertyBridge;");
		this.out.println("\td2rq:belongsToClassMap " + classMapName(firstForeignColumn.getTableName()) + ";");
		this.out.println("\td2rq:property vocab:" + linkTableName + ";");
		this.out.println("\td2rq:refersToClassMap " + classMapName(secondForeignColumn.getTableName()) + ";");
		this.out.println("\td2rq:join \"" + firstForeignColumn.getQualifiedName() + " = " + firstLocalColumn.getQualifiedName() + "\";");
		this.out.println("\td2rq:join \"" + secondLocalColumn.getQualifiedName() + " = " + secondForeignColumn.getQualifiedName() + "\";");
		this.out.println("\t.");
		createLinkProperty(linkTableName, firstForeignColumn.getTableName(), secondForeignColumn.getTableName());
	}

	private String classMapName(String tableName) {
		return ":classMap_" + tableName;
	}
	
	private boolean hasPrimaryKey(String tableName) {
		return !this.schema.primaryKeyColumns(tableName).isEmpty();
	}
	
	private String uriPattern(String tableName) {
		String result = this.instanceNamespaceURI + tableName;
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result += "/@@" + column.getQualifiedName() + "@@";
		}
		return result;
	}
	
	private String name(Column column) {
		return column.getTableName() + "_" + column.getColumnName();
	}

	private void identifyLinkTables() {
		Iterator it = this.schema.listTableNames().iterator();
		while (it.hasNext()) {
			String tableName = (String) it.next();
			if (!this.schema.isLinkTable(tableName)) {
				continue;
			}
			Column[] firstForeignKey = (Column[]) this.schema.foreignKeyColumns(tableName).get(0);
			this.linkTables.put(tableName, firstForeignKey[1].getTableName());
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

	private void createVocabularyClass(String tableName) {
		Resource r = classResource(tableName);
		r.addProperty(RDF.type, RDFS.Class);
		r.addProperty(RDF.type, OWL.Class);
		r.addProperty(RDFS.label, tableName);
		r.addProperty(RDFS.isDefinedBy, ontologyResource());
	}

	private void initProperty(Resource r, Column column) {
		r.addProperty(RDF.type, RDF.Property);
		r.addProperty(RDFS.label, name(column));
		r.addProperty(RDFS.domain, classResource(column.getTableName()));
		r.addProperty(RDFS.isDefinedBy, ontologyResource());		
	}
	
	private void createDatatypeProperty(Column column, RDFDatatype datatype) {
		Resource r = propertyResource(column);
		initProperty(r, column);
		r.addProperty(RDF.type, OWL.DatatypeProperty);
		if (datatype != null) {
			r.addProperty(RDFS.range, this.vocabModel.getResource(datatype.getURI()));
		}
	}

	private void createObjectProperty(Column subjectColumn, Column objectColumn) {
		Resource r = propertyResource(subjectColumn);
		initProperty(r, subjectColumn);
		r.addProperty(RDF.type, OWL.ObjectProperty);
		r.addProperty(RDFS.range, classResource(objectColumn.getTableName()));
	}

	private void createLinkProperty(String linkTableName, String fromTable, String toTable) {
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
	
	private Resource classResource(String tableName) {
		String classURI = this.vocabNamespaceURI + tableName;
		return this.vocabModel.createResource(classURI);		
	}

	private Resource propertyResource(Column column) {
		String propertyURI = this.vocabNamespaceURI + name(column);
		return this.vocabModel.createResource(propertyURI);
	}
	
	private String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}