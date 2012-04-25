package de.fuberlin.wiwiss.d2rq.dbschema;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class ISWCSchemaTest extends TestCase {
	private final static String driverClass = "com.mysql.jdbc.Driver";
	private final static String jdbcURL = "jdbc:mysql://127.0.0.1/iswc?user=root";
	
	private ConnectedDB db;
	
	public void setUp() throws Exception {
		Class.forName(driverClass);
		db = new ConnectedDB(jdbcURL, null, null);
	}
	
	public void tearDown() {
		db.close();
	}
	
	public void testRecognizeNullableColumn() {
		Attribute personEmail = new Attribute(null, "persons", "Email");
		assertTrue(db.isNullable(personEmail));
	}
	
	public void testRecognizeNonNullableColumn() {
		Attribute personID = new Attribute(null, "persons", "PerID");
		assertFalse(db.isNullable(personID));
	}
}
