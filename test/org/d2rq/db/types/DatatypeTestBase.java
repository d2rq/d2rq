package org.d2rq.db.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.d2rq.CompiledMapping;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.ClassMap;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.d2rq.lang.PropertyBridge;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


public abstract class DatatypeTestBase {
	private final static String EX = "http://example.com/";
	private final static Resource dbURI = ResourceFactory.createResource(EX + "db");
	private final static Resource classMapURI = ResourceFactory.createResource(EX + "classmap");
	private final static Resource propertyBridgeURI = ResourceFactory.createResource(EX + "propertybridge");
	private final static Resource valueProperty = ResourceFactory.createProperty(EX + "value");
	
	private String jdbcURL;
	private String driver;
	private String user;
	private String password;
	private String script;
	
	private String datatype;
	private SQLConnection sqlConnection;
	private CompiledMapping mapping;
	private GraphD2RQ graph;
	
	public void tearDown() {
		if (graph != null) graph.close();
	}
	
	protected void initDB(String jdbcURL, String driver, 
			String user, String password, String script) {
		this.jdbcURL = jdbcURL;
		this.driver = driver;
		this.user = user;
		this.password = password;
		this.script = script;
	}
	
	protected void createMapping(String datatype) {
		this.datatype = datatype;
		Mapping map = generateMapping();
		map.configuration().setServeVocabulary(false);
		mapping = map.compile();
		graph = getGraph(mapping);
		mapping.connect();
		sqlConnection = mapping.getSQLConnections().iterator().next();
	}
	
	protected void assertMappedType(String rdfType) {
		assertEquals(PrefixMapping.Standard.expandPrefix(rdfType),
				sqlConnection.getTable(TableName.parse("T_" + datatype))
				.getColumnType(ColumnName.parse("VALUE")).rdfType());
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

	private GraphD2RQ getGraph(CompiledMapping mapping) {
		return new GraphD2RQ(mapping);
	}
	
	private Mapping generateMapping() {
		Mapping mapping = new Mapping();
		Database database = new Database(dbURI);
		database.setJdbcURL(jdbcURL);
		database.setJDBCDriver(driver);
		database.setUsername(user);
		database.setPassword(password);
		if (script != null) {
			database.setStartupSQLScript(ResourceFactory.createResource("file:" + script));
		}
		mapping.addDatabase(database);
		ClassMap classMap = new ClassMap(classMapURI);
		classMap.setDatabase(database);
		classMap.setURIPattern("row/@@T_" + datatype + ".ID@@");
		mapping.addClassMap(classMap);
		PropertyBridge propertyBridge = new PropertyBridge(propertyBridgeURI);
		propertyBridge.setBelongsToClassMap(classMap);
		propertyBridge.addProperty(valueProperty);
		propertyBridge.setColumn("T_" + datatype + ".VALUE");
		return mapping;
	}
}
