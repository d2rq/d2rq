package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class MappingTest extends TestCase {
	private final static Resource myDB = ResourceFactory.createResource("http://test/db");
	private final static Resource classMap1 = ResourceFactory.createResource("http://test/classMap1");
	
	public void testNewMappingWithResource() {
		Mapping m = new Mapping("http://test/mapping.n3");
		assertEquals(ResourceFactory.createResource("http://test/mapping.n3"), m.resource());
	}

	public void testNewMappingWithoutResource() {
		Mapping m = new Mapping();
		assertTrue(m.resource().isAnon());
	}
	
	public void testProcessingInstructions() {
		Mapping m = new Mapping();
		assertNull(m.processingInstruction(D2RQ.queryHandler));
		m.setProcessingInstruction(D2RQ.queryHandler, "foo");
		assertEquals("foo", m.processingInstruction(D2RQ.queryHandler));
	}
	
	public void testNoDatabasesInitially() {
		Mapping m = new Mapping();
		assertTrue(m.databases().isEmpty());
		assertNull(m.database(myDB));
	}
	
	public void testReturnAddedDatabase() {
		Mapping m = new Mapping();
		Database db = new Database("odbc", null, null, null, null, Collections.EMPTY_MAP);
		m.addDatabase(myDB, db);
		assertEquals(Collections.singletonList(db), new ArrayList(m.databases()));
		assertEquals(db, m.database(myDB));
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
		Database db = new Database("odbc", null, null, null, null, Collections.EMPTY_MAP);
		ClassMap c = new ClassMap(classMap1);
		c.setDatabase(db);
		assertEquals(db, c.database());
	}
	
	public void testMultipleDatabasesForClassMapCauseValidationError() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		try {
			Database db1 = new Database("odbc1", null, null, null, null, Collections.EMPTY_MAP);
			c.setDatabase(db1);
			Database db2 = new Database("odbc2", null, null, null, null, Collections.EMPTY_MAP);
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
			Database db1 = new Database("odbc1", null, null, null, null, Collections.EMPTY_MAP);
			m.addDatabase(myDB, db1);
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
				new HashSet(m.classMapResources()));
		assertEquals(c, m.classMap(classMap1));
	}
}
