package org.d2rq.lang;

import static org.junit.Assert.*;

import org.d2rq.D2RQException;
import org.d2rq.lang.ClassMap;
import org.d2rq.lang.D2RQValidator;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class D2RQValidatorTest {
	private Resource database1, database2, classMap1;

	@Before
	public void setUp() throws Exception {
		database1 = ResourceFactory.createResource("http://test/db");
		database2 = ResourceFactory.createResource("http://test/db2");
		classMap1 = ResourceFactory.createResource("http://test/classMap1");
	}

	@Test
	public void testNoDatabaseCausesValidationError() {
		Mapping m = new Mapping();
		try {
			new D2RQValidator(m).run();
		} catch (D2RQException ex) {
			assertTrue(ex.getMessage().contains("MAPPING_NO_DATABASE"));
		}
	}
	
	@Test
	public void testMultipleDatabasesForClassMapCauseValidationError() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		try {
			Database db1 = new Database(database1);
			c.setDatabase(db1);
			Database db2 = new Database(database2);
			c.setDatabase(db2);
			m.addClassMap(c);
			new D2RQValidator(m).run();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.CLASSMAP_DUPLICATE_DATABASE, ex.errorCode());
		}
	}
	
	@Test
	public void testClassMapWithoutDatabaseCausesValidationError() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		try {
			Database db1 = new Database(database1);
			db1.setJdbcURL("jdbc:mysql:///db");
			db1.setJDBCDriver("org.example.Driver");
			m.addDatabase(db1);
			m.addClassMap(c);
			new D2RQValidator(m).run();
		} catch (D2RQException ex) {
			assertTrue(ex.getMessage().contains("CLASSMAP_NO_DATABASE"));
		}
	}
}
