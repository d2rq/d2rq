package de.fuberlin.wiwiss.d2rq.mapgen;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;

import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.DatabaseSchemaInspector;

public class MappingGenerator {
	private String jdbcURL;
	private String prefix;
	private String driverClass = null;
	private String databaseUser = null;
	private String databasePassword = null;
	private DatabaseSchemaInspector schema = null;
	private String databaseType = null;

	public MappingGenerator(String jdbcURL) {
		this.jdbcURL = jdbcURL;
		this.prefix = this.jdbcURL + "#";
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
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
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
	
	public void write(PrintStream out) {
		if (this.schema == null) {
			connectToDatabase();
		}
		out.println("@prefix : <#> .");
		out.println("@prefix db: <" + this.prefix + "> .");
		out.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		out.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		out.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		out.println();
		writeDatabase(out);
		Iterator it = schema.listTableNames().iterator();
		while (it.hasNext()) {
			String tableName = (String) it.next();
			writeTable(out, tableName);
		}
	}
	
	public void writeDatabase(PrintStream out) {
		out.println(":database a d2rq:Database;");
		out.println("\td2rq:jdbcDriver \"" + this.driverClass + "\";");
		out.println("\td2rq:jdbcDSN \"" + this.jdbcURL + "\";");
		if (this.databaseUser != null) {
			out.println("\td2rq:username \"" + this.databaseUser + "\";");
		}
		if (this.databasePassword != null) {
			out.println("\td2rq:password \"" + this.databasePassword + "\";");
		}
		out.println("\t.");
		out.println();
	}
	
	public void writeTable(PrintStream out, String tableName) {
		out.println("# Table " + tableName);
		out.println(classMapName(tableName) + " a d2rq:ClassMap;");
		out.println("\td2rq:dataStorage :database;");
		out.println("\td2rq:uriPattern \"" + uriPattern(tableName) + "\";");
		out.println("\td2rq:class db:" + tableName + ";");
		out.println("\t.");
		Iterator it = this.schema.listColumns(tableName).iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			writeColumn(out, column, classMapName(tableName));
		}
		out.println();
	}
	
	public void writeColumn(PrintStream out, Column column, String belongsToClassMap) {
		out.println(":propertyBridge_" + name(column) + " a d2rq:PropertyBridge;");
		out.println("\td2rq:belongsToClassMap " + belongsToClassMap + ";");
		out.println("\td2rq:property db:" + name(column) + ";");
		out.println("\td2rq:column \"" + column.getQualifiedName() + "\";");
		writeColumnHacks(out, column);
		out.println("\t.");
	}

	// TODO Factor out into its own interface & classes for different RDBMS?
	public void writeColumnHacks(PrintStream out, Column column) {
		int colType = this.schema.columnType(column);
		String xsd = DatabaseSchemaInspector.xsdTypeFor(colType);
		if (xsd != null) {
			out.println("\td2rq:datatype " + xsd + ";");
		}
//		if (DatabaseSchemaInspector.isStringType(colType)) {
//			// Suppress empty strings ('')
//			out.println("\td2rq:condition \"" + column.getQualifiedName() + " != ''\";");			
//		}
		if (this.databaseType == "MySQL" && DatabaseSchemaInspector.isDateType(colType)) {
			// Work around an issue with the MySQL driver where SELECTing a date/time
			// column containing '0000-00-00 ...' causes an SQLException
			out.println("\td2rq:condition \"" + column.getQualifiedName() + " != '0000'\";");
		}
	}
	
	private String classMapName(String tableName) {
		return ":classMap_" + tableName;
	}
	
	private String uriPattern(String tableName) {
		// TODO: Handle tables without primary key
		String result = prefix + tableName;
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result += "@@" + column.getQualifiedName() + "@@";
			if (it.hasNext()) {
				result += "-";
			}
		}
		return result;
	}
	
	private String name(Column column) {
		return column.getTableName() + "_" + column.getColumnName();
	}
}