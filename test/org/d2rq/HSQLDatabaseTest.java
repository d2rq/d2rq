package org.d2rq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.TableName;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.Mapping;
import org.d2rq.mapgen.D2RQMappingStyle;
import org.d2rq.mapgen.D2RQTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.RDF;


/**
 * A set of simple tests that apply various parts of D2RQ to
 * an HSQL database, exercising our test helpers like
 * {@link HSQLDatabase} and {@link MappingHelper}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class HSQLDatabaseTest {
	private final static String EX = "http://example.org/";
	private HSQLDatabase db;
	
	@Before
	public void setUp() throws Exception {
		db = new HSQLDatabase("test");
		db.executeSQL("CREATE TABLE TEST (ID INT PRIMARY KEY, VALUE VARCHAR(50) NULL)");
	}
	
	@After
	public void tearDown() {
		db.close(true);
	}
	
	@Test
	public void testFindTableWithSchemaInspector() {
		SQLConnection sqlConnection = new SQLConnection(
				db.getJdbcURL(), HSQLDatabase.DRIVER_CLASS, 
				db.getUser(), db.getPassword());
		assertEquals(new ArrayList<TableName>() {{ 
					add(TableName.parse("TEST"));
				}}, sqlConnection.getTableNames(null));
	}
	
	@Test
	public void testGenerateSomeClassMapsInDefaultMapping() {
		Mapping mapping = generateDefaultMapping();
		assertEquals(1, mapping.classMapResources().size());
	}
	
	@Test
	public void testDefaultMappingWithHelloWorld() {
		db.executeSQL("INSERT INTO TEST VALUES (1, 'Hello World!')");
		GraphD2RQ g = generateDefaultGraphD2RQ();
		assertTrue(g.contains(Node.ANY, Node.ANY, Node.createLiteral("Hello World!")));
	}
	
	@Test
	public void testGenerateEmptyGraphFromSimpleD2RQMapping() {
		Mapping m = D2RQTestSuite.loadMapping("simple.ttl");
		m.configuration().setServeVocabulary(false);
		GraphD2RQ g = new GraphD2RQ(m.compile());
		assertTrue(g.isEmpty());
	}
	
	@Test
	public void testGenerateTripleFromSimpleD2RQMapping() {
		Mapping m = D2RQTestSuite.loadMapping("simple.ttl");
		m.configuration().setServeVocabulary(false);
		db.executeSQL("INSERT INTO TEST VALUES (1, 'Hello World!')");
		GraphD2RQ g = new GraphD2RQ(m.compile());
		assertTrue(g.contains(
				Node.createURI(EX + "test/1"), RDF.Nodes.type, Node.createURI(EX + "Test")));
		assertEquals(1, g.size());
	}
	
	private Mapping generateDefaultMapping() {
		SQLConnection sqlConnection = new SQLConnection(
				db.getJdbcURL(), HSQLDatabase.DRIVER_CLASS,
				db.getUser(), null);
		D2RQTarget target = new D2RQTarget();
		new D2RQMappingStyle(sqlConnection, EX).getMappingGenerator().generate(target);
		return target.getMapping();
	}
	
	private GraphD2RQ generateDefaultGraphD2RQ() {
		return new GraphD2RQ(generateDefaultMapping().compile());
	}
}
