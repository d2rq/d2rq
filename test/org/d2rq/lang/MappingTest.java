package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.d2rq.lang.ClassMap;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class MappingTest {
	private Resource database1, classMap1;
	
	@Before
	public void setUp() {
		database1 = ResourceFactory.createResource("http://test/db");
		classMap1 = ResourceFactory.createResource("http://test/classMap1");
	}
	
	@Test
	public void testNewMappingWithResource() {
		Mapping m = new Mapping("http://test/mapping.ttl");
		assertEquals(ResourceFactory.createResource("http://test/mapping.ttl"), m.resource());
	}

	@Test
	public void testNewMappingWithoutResource() {
		Mapping m = new Mapping();
		assertTrue(m.resource().isAnon());
	}
	
	@Test
	public void testNoDatabasesInitially() {
		Mapping m = new Mapping();
		assertTrue(m.databases().isEmpty());
		assertNull(m.database(database1));
	}
	
	@Test
	public void testReturnAddedDatabase() {
		Mapping m = new Mapping();
		Database db = new Database(database1);
		m.addDatabase(db);
		assertEquals(Collections.singletonList(db), new ArrayList<Database>(m.databases()));
		assertEquals(db, m.database(database1));
	}
	
	@Test
	public void testReturnResourceFromNewClassMap() {
		ClassMap c = new ClassMap(classMap1);
		assertEquals(classMap1, c.resource());
	}
	
	@Test
	public void testNewClassMapHasNoDatabase() {
		ClassMap c = new ClassMap(classMap1);
		assertNull(c.getDatabase());
	}
	
	@Test
	public void testClassMapReturnsAssignedDatabase() {
		Database db = new Database(database1);
		ClassMap c = new ClassMap(classMap1);
		c.setDatabase(db);
		assertEquals(db, c.getDatabase());
	}
	
	@Test
	public void testNewMappingHasNoClassMaps() {
		Mapping m = new Mapping();
		assertTrue(m.classMapResources().isEmpty());
		assertNull(m.classMap(classMap1));
	}
	
	@Test
	public void testReturnAddedClassMaps() {
		Mapping m = new Mapping();
		ClassMap c = new ClassMap(classMap1);
		m.addClassMap(c);
		assertEquals(Collections.singleton(classMap1), 
				new HashSet<Resource>(m.classMapResources()));
		assertEquals(c, m.classMap(classMap1));
	}
}
