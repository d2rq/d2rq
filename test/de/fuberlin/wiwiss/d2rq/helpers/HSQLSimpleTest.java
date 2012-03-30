package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.ArrayList;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * A set of simple tests that apply various parts of D2RQ to
 * an HSQL database, exercising our test helpers like
 * {@link HSQLDatabase} and {@link MappingHelper}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class HSQLSimpleTest extends TestCase {
	private final static String EX = "http://example.org/";

	{
		ConnectedDB.registerJDBCDriver("org.hsqldb.jdbcDriver");
	}
	
	private HSQLDatabase db;
	
	public void setUp() {
		db = new HSQLDatabase("test");
		db.executeSQL("CREATE TABLE TEST (ID INT PRIMARY KEY, VALUE VARCHAR(50) NULL)");
	}
	
	public void tearDown() {
		db.close(true);
	}
	
	public void testFindTableWithSchemaInspector() {
		DatabaseSchemaInspector schema = 
			new DatabaseSchemaInspector(
					new ConnectedDB(
							db.getJdbcURL(), db.getUser(), db.getPassword()));
		assertEquals(new ArrayList<RelationName>() {{ 
					add(new RelationName(null, "TEST"));
				}}, schema.listTableNames(null));
	}
	
	public void testGenerateDefaultMappingModel() {
		Model model = generateDefaultMappingModel();
		assertFalse(model.isEmpty());
	}
	
	public void testGenerateSomeClassMapsInDefaultMapping() {
		Mapping mapping = generateDefaultMapping();
		assertEquals(1, mapping.classMapResources().size());
	}
	
	public void testDefaultMappingWithHelloWorld() {
		db.executeSQL("INSERT INTO TEST VALUES (1, 'Hello World!')");
		GraphD2RQ g = generateDefaultGraphD2RQ();
		assertTrue(g.contains(Node.ANY, Node.ANY, Node.createLiteral("Hello World!")));
	}
	
	public void testGenerateEmptyGraphFromSimpleD2RQMapping() {
		Mapping m = MappingHelper.readFromTestFile("helpers/simple.ttl");
		m.configuration().setServeVocabulary(false);
		GraphD2RQ g = new GraphD2RQ(m);
		assertTrue(g.isEmpty());
	}
	
	public void testGenerateTripleFromSimpleD2RQMapping() {
		Mapping m = MappingHelper.readFromTestFile("helpers/simple.ttl");
		m.configuration().setServeVocabulary(false);
		db.executeSQL("INSERT INTO TEST VALUES (1, 'Hello World!')");
		GraphD2RQ g = new GraphD2RQ(m);
		assertTrue(g.contains(
				Node.createURI(EX + "test/1"), RDF.Nodes.type, Node.createURI(EX + "Test")));
		assertEquals(1, g.size());
	}
	
	private Model generateDefaultMappingModel() {
		ConnectedDB cdb = new ConnectedDB(db.getJdbcURL(), db.getUser(), null);
		MappingGenerator generator = new MappingGenerator(cdb);
		return generator.mappingModel(EX);
	}
	
	private Mapping generateDefaultMapping() {
		return new MapParser(generateDefaultMappingModel(), EX).parse();
	}
	
	private GraphD2RQ generateDefaultGraphD2RQ() {
		return new GraphD2RQ(generateDefaultMapping());
	}
}
