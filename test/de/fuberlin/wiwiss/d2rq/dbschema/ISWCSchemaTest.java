package de.fuberlin.wiwiss.d2rq.dbschema;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.DriverConnectedDB;

public class ISWCSchemaTest extends TestCase {
	private final static String driverClass = "com.mysql.jdbc.Driver";
	private final static String jdbcURL = "jdbc:mysql://127.0.0.1/iswc?user=root&password=root";
	
	private DatabaseSchemaInspector schema;
	
	public void setUp() throws Exception {
		Class.forName(driverClass);
		ConnectedDB db = new DriverConnectedDB(jdbcURL, null, null);
		db.init();
		this.schema = db.schemaInspector();
	}
	
	public void testRecognizeNullableColumn() {
		Attribute personEmail = new Attribute(null, "persons", "Email");
		assertTrue(this.schema.isNullable(personEmail));
	}
	
	public void testRecognizeNonNullableColumn() {
		Attribute personID = new Attribute(null, "persons", "PerID");
		assertFalse(this.schema.isNullable(personID));
	}
}
