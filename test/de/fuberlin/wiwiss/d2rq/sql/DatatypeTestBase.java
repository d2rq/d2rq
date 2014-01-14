package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

public abstract class DatatypeTestBase extends TestCase {
	private final static String EX = "http://example.com/";
	private final static Resource dbURI = ResourceFactory.createResource(EX + "db");
	private final static Resource classMapURI = ResourceFactory.createResource(EX + "classmap");
	private final static Resource propertyBridgeURI = ResourceFactory.createResource(EX + "propertybridge");
	private final static Resource valueProperty = ResourceFactory.createProperty(EX + "value");
	
	private String jdbcURL;
	private String driver;
	private String user;
	private String password;
	private String schema;
	private String script;
	
	private String datatype;
	private GraphD2RQ graph;
	private DatabaseSchemaInspector inspector;
	
	public void tearDown() {
		if (graph != null) graph.close();
	}
	
	protected void initDB(String jdbcURL, String driver, 
			String user, String password, String script, String schema) {
		this.jdbcURL = jdbcURL;
		this.driver = driver;
		this.user = user;
		this.password = password;
		this.script = script;
		this.schema = null;
		dropAllTables();
	}
	
	private void dropAllTables() {
		ConnectedDB.registerJDBCDriver(driver);
		ConnectedDB db = new ConnectedDB(jdbcURL, user, password);
		try {
			Statement stmt = db.connection().createStatement();
			try {
				for (String table: allTables()) {
					stmt.execute("DROP TABLE " + table);
				}
			} finally {
				db.vendor().beforeClose(db.connection());
				stmt.close();
				db.vendor().afterClose(db.connection());
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		} finally {
			db.close();
		}
	}
	
	protected void createMapping(String datatype) {
		this.datatype = datatype;
		Mapping mapping = generateMapping();
		mapping.configuration().setServeVocabulary(false);
		mapping.configuration().setUseAllOptimizations(true);
		mapping.connect();
		graph = getGraph(mapping);
		inspector = mapping.databases().iterator().next().connectedDB().schemaInspector();
	}
	
	protected void assertMappedType(String rdfType) {
		assertEquals(rdfType, inspector.columnType(
				SQL.parseAttribute("T_" + datatype + ".VALUE")).rdfType());
	}
	
	protected void assertValues(String[] expectedValues) {
		assertValues(expectedValues, true);
	}
	
	protected void assertValues(String[] expectedValues, boolean searchValues) {
		ExtendedIterator<Triple> it = graph.find(Node.ANY, Node.ANY, Node.ANY);
		List<String> listedValues = new ArrayList<String>();
		while (it.hasNext()) {
			listedValues.add(it.next().getObject().getLiteralLexicalForm());
		}
		assertEquals(Arrays.asList(expectedValues), listedValues);
		if (!searchValues) return;
		for (String value : expectedValues) {
			assertTrue("Expected literal not in graph: '" + value + "'",
					graph.contains(Node.ANY, Node.ANY, Node.createLiteral(value)));
		}
	}

	protected void assertValuesNotFindable(String[] expectedValues) {
		for (String value : expectedValues) {
			assertFalse("Unexpected literal found in graph: '" + value + "'",
					graph.contains(Node.ANY, Node.ANY, Node.createLiteral(value)));
		}
	}

	private Set<String> allTables() {
		ConnectedDB.registerJDBCDriver(driver);
		ConnectedDB db = new ConnectedDB(jdbcURL, user, password);
		try {
			Set<String> result = new HashSet<String>();
			inspector = db.schemaInspector();
			for (RelationName name: inspector.listTableNames(schema)) {
				result.add(name.toString());
			}
			return result;
		} finally {
			db.close();
		}
	}
	
	private GraphD2RQ getGraph(Mapping mapping) {
		return new GraphD2RQ(mapping);
	}
	
	private Mapping generateMapping() {
		Mapping mapping = new Mapping();
		Database database = new Database(dbURI);
		database.setJDBCDSN(jdbcURL);
		database.setJDBCDriver(driver);
		database.setUsername(user);
		database.setPassword(password);
		database.setStartupSQLScript(ResourceFactory.createResource("file:" + script));
		mapping.addDatabase(database);
		ClassMap classMap = new ClassMap(classMapURI);
		classMap.setDatabase(database);
		classMap.setURIPattern("row/@@T_" + datatype + ".ID@@");
		mapping.addClassMap(classMap);
		PropertyBridge propertyBridge = new PropertyBridge(propertyBridgeURI);
		propertyBridge.setBelongsToClassMap(classMap);
		propertyBridge.addProperty(valueProperty);
		propertyBridge.setColumn("T_" + datatype + ".VALUE");
		classMap.addPropertyBridge(propertyBridge);
		return mapping;
	}
}
