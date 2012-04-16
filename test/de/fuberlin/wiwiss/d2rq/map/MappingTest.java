package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;

public class MappingTest extends TestCase {
	private final static Resource database1 = ResourceFactory.createResource("http://test/db");
	private final static Resource database2 = ResourceFactory.createResource("http://test/db2");
	private final static Resource classMap1 = ResourceFactory.createResource("http://test/classMap1");
	
	public void testNewMappingWithResource() {
		Mapping m = new Mapping("http://test/mapping.ttl");
		assertEquals(ResourceFactory.createResource("http://test/mapping.ttl"), m.resource());
	}

	public void testNewMappingWithoutResource() {
		Mapping m = new Mapping();
		assertTrue(m.resource().isAnon());
	}
	
	public void testNoDatabasesInitially() {
		Mapping m = new Mapping();
		assertTrue(m.databases().isEmpty());
		assertNull(m.database(database1));
	}
	
	public void testReturnAddedDatabase() {
		Mapping m = new Mapping();
		Database db = new Database(database1);
		m.addDatabase(db);
		assertEquals(Collections.singletonList(db), new ArrayList<Database>(m.databases()));
		assertEquals(db, m.database(database1));
	}
	
	public void testNoDatabaseCausesValidationError() {
		Mapping m = new Mapping();
		try {
			m.validate();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_NO_DATABASE, ex.errorCode());
		}
	}
	
	public void testReturnResourceFromNewClassMap() {
		ClassMap c = new ClassMap(classMap1);
		assertEquals(classMap1, c.resource());
	}
	
	public void testNewClassMapHasNoDatabase() {
		ClassMap c = new ClassMap(classMap1);
		assertNull(c.database());
	}
	
	public void testClassMapReturnsAssignedDatabase() {
		Database db = new Database(database1);
		ClassMap c = new ClassMap(classMap1);
		c.setDatabase(db);
		assertEquals(db, c.database());
	}
	
	public void testMultipleDatabasesForClassMapCauseValidationError() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		try {
			Database db1 = new Database(database1);
			c.setDatabase(db1);
			Database db2 = new Database(database2);
			c.setDatabase(db2);
			m.addClassMap(c);
			m.validate();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.CLASSMAP_DUPLICATE_DATABASE, ex.errorCode());
		}
	}
	
	public void testClassMapWithoutDatabaseCausesValidationError() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		try {
			Database db1 = new Database(database1);
			db1.setJDBCDSN("jdbc:mysql:///db");
			db1.setJDBCDriver("org.example.Driver");
			m.addDatabase(db1);
			m.addClassMap(c);
			m.validate();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.CLASSMAP_NO_DATABASE, ex.errorCode());
		}
	}
	
	public void testNewMappingHasNoClassMaps() {
		Mapping m = new Mapping();
		assertTrue(m.classMapResources().isEmpty());
		assertNull(m.classMap(classMap1));
	}
	
	public void testReturnAddedClassMaps() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		m.addClassMap(c);
		assertEquals(Collections.singleton(classMap1), 
				new HashSet<Resource>(m.classMapResources()));
		assertEquals(c, m.classMap(classMap1));
	}
}
