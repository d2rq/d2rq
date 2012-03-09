package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.helpers.HSQLDatabase;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

/*
 * TODO: check that querying for some other valid literals doesn't lead to errors
 * (e.g., for an xsd:dateTime with time zone on a non-timezoned TIMESTAMP column, 
 * or 'abcd' on BIT, or five-digit or negative year on xsd:date, or too long VARCHAR,
 * or generally anything that's a valid XSD literal but not a valid SQL literal of
 * the equivalent type)
 */
public class HSQLDBDatatypeTest extends TestCase {
	private final static String EX = "http://example.com/";
	private final static Resource dbURI = ResourceFactory.createResource(EX + "db");
	private final static Resource classMapURI = ResourceFactory.createResource(EX + "classmap");
	private final static Resource propertyBridgeURI = ResourceFactory.createResource(EX + "propertybridge");
	private final static Resource valueProperty = ResourceFactory.createProperty(EX + "value");
	
	private HSQLDatabase db;
	private GraphD2RQ graph;
	
	public void setUp() {
		db = new HSQLDatabase("test");
		db.executeScript(D2RQTestSuite.DIRECTORY + "sql/hsqldb_datatypes.sql");
	}

	public void tearDown() {
		db.close(true);
		if (graph != null) graph.close();
	}
	
	public void testTinyInt() {
		assertValues("TINYINT", new String[]{"0", "1", "-128", "127"});
	}
	
	public void testSmallInt() {
		assertValues("SMALLINT", new String[]{"0", "1", "-32768", "32767"});
	}
	
	public void testInteger() {
		assertValues("INTEGER", new String[]{"0", "1", "-2147483648", "2147483647"});
	}
	
	public void testBigInt() {
		assertValues("BIGINT", new String[]{"0", "1", "-9223372036854775808", "9223372036854775807"});
	}

	public void testNumeric() {
		String[] expected = new String[]{"0", "1", "1000000000000000000", "-1000000000000000000"};
		assertValues("NUMERIC", expected);
		assertValues("DECIMAL", expected);
	}
	
	public void testDecimal_4_2() {
		assertValues("DECIMAL_4_2", new String[]{"0", "1", "4.95", "99.99", "-99.99"});
	}

	public void testDouble() {
		String[] expected = {"0.0E0", "1.0E0", "1.0E8", "1.0E-7", "-0.0E0", "INF", "-INF"}; 
		assertValues("DOUBLE", expected);
		assertValues("REAL", expected);
		assertValues("FLOAT", expected);
	}
	
	public void testBoolean() {
		assertValues("BOOLEAN", new String[]{"false", "true"});
	}

	public void testChar_3() {
		assertValues("CHAR_3", new String[]{"   ", "AOU", "\u00C4\u00D6\u00DC"});
	}
	
	public void testChar() {
		assertValues("CHAR", new String[]{" ", "A", "\u00C4"});
	}
	
	public void testVarchar() {
		String[] expected = new String[]{"", "AOU", "\u00C4\u00D6\u00DC"};
		assertValues("VARCHAR", expected);
		assertValues("LONGVARCHAR", expected);
	}
	
	public void testCLOB() {
		assertValues("CLOB", new String[]{"AOU", "\u00C4\u00D6\u00DC"});
	}
	
	public void testBinary_4() {
		assertValues("BINARY_4", new String[] {"00000000", "FFFFFFFF", "F001F001"});
	}
	
	public void testBinary() {
		assertValues("BINARY", new String[] {"00", "01", "FF"});
	}
	
	public void testVarBinary() {
		String[] expected = new String[] {"", "00", "01", "F001F001F001F001"};
		assertValues("VARBINARY", expected);
		assertValues("LONGVARBINARY", expected);
	}

	public void testBLOB() {
		assertValues("BLOB", new String[] {"00", "01", "F001F001F001F001"});
	}
	
	public void testBit_4() {
		assertValues("BIT_4", new String[] {"0000", "0001", "1000", "1111"});
	}

	public void testBit() {
		assertValues("BIT", new String[] {"0", "1"});
	}

	public void testBitVarying() {
		assertValues("BIT_VARYING", new String[] {"", "0", "1", "100000000000000"});
	}

	public void testDate() {
		assertValues("DATE", new String[] {"0001-01-01", "2012-03-07", "9999-12-31"});
	}

	public void testTime() {
		assertValues("TIME", new String[]{"00:00:00", "20:39:21", "23:59:59"});
	}

	public void testTime_4() {
		assertValues("TIME_4", new String[]{"00:00:00", "20:39:21.5831", "23:59:59.9999"});
	}

	public void testTimeTZ() {
		assertValues("TIME_TZ", new String[]{
				"00:00:00Z", "20:39:21Z", "23:59:59Z", 
				"20:39:21Z", "20:39:21-01:00", "20:39:21+11:30"});
	}

	public void testTimeTZ_4() {
		assertValues("TIME_TZ_4", new String[]{
				"00:00:00Z", "20:39:21.5831Z", "23:59:59.9999Z", 
				"20:39:21.5831Z", "20:39:21.5831-01:00", "20:39:21.5831+11:30"});
	}
	
	public void testTimestamp() {
		assertValues("TIMESTAMP", new String[]{
				"0001-01-01T00:00:00", 
				"2012-03-07T20:39:21.583122", 
				"9999-12-31T23:59:59.999999"});
	}

	public void testTimestamp_4() {
		assertValues("TIMESTAMP_4", new String[]{
				"0001-01-01T00:00:00", 
				"2012-03-07T20:39:21.5831", 
				"9999-12-31T23:59:59.9999"});
	}

	public void testTimestampTZ() {
		assertValues("TIMESTAMP_TZ", new String[]{
				"0001-01-01T00:00:00Z", 
				"2012-03-07T20:39:21.583122Z", 
				"9999-12-31T23:59:59.999999Z", 
				"2012-03-07T20:39:21.583122Z", 
				"2012-03-07T20:39:21.583122-01:00", 
				"2012-03-07T20:39:21.583122+11:30"});
	}

	public void testTimestampTZ_4() {
		assertValues("TIMESTAMP_TZ_4", new String[]{
				"0001-01-01T00:00:00Z", 
				"2012-03-07T20:39:21.5831Z", 
				"9999-12-31T23:59:59.9999Z", 
				"2012-03-07T20:39:21.5831Z", 
				"2012-03-07T20:39:21.5831-01:00", 
				"2012-03-07T20:39:21.5831+11:30"});
	}

	public void testIntervalDay() {
		assertValues("INTERVAL_DAY", new String[]{"0", "1", "99", "-99"}, false);
	}

	public void testIntervalHourMinute() {
		assertValues("INTERVAL_HOUR_MINUTE", new String[]{
				"0:00", "0:01", "1:00", "99:00", "-99:00"}, false);
	}

	public void testOther() {
		assertUnmappable("OTHER");
	}
	
	public void testArrayInteger() {
		assertUnmappable("ARRAY_INTEGER");
	}
	
	private void assertUnmappable(String datatype) {
		try {
			graph = getGraph(datatype);
			graph.find(RDF.Nodes.Alt, RDF.Nodes.Alt, RDF.Nodes.Alt);
			fail("Should fail due to DATATYPE_UNMAPPABLE for '" + datatype + "'");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.DATATYPE_UNMAPPABLE, ex.errorCode());
		}
	}
	
	private void assertValues(String datatype, String[] expectedValues) {
		assertValues(datatype, expectedValues, true);
	}
	
	private void assertValues(String datatype, String[] expectedValues, boolean searchable) {
		graph = getGraph(datatype);
		ExtendedIterator<Triple> it = graph.find(Node.ANY, Node.ANY, Node.ANY);
		List<String> listedValues = new ArrayList<String>();
		while (it.hasNext()) {
			listedValues.add(it.next().getObject().getLiteralLexicalForm());
		}
		assertEquals(Arrays.asList(expectedValues), listedValues);
		for (String value : expectedValues) {
			if (searchable) {
				assertTrue("Expected literal not in graph: '" + value + "'",
						graph.contains(Node.ANY, Node.ANY, Node.createLiteral(value)));
			} else {
				assertFalse("Unexpected literal found in graph: '" + value + "'",
						graph.contains(Node.ANY, Node.ANY, Node.createLiteral(value)));
			}
		}
	}
	
	private GraphD2RQ getGraph(String datatype) {
		GraphD2RQ result = new GraphD2RQ(generateMapping(datatype));
		result.getConfiguration().setServeVocabulary(false);
		result.getConfiguration().setUseAllOptimizations(true);
		return result;
	}
	
	private Mapping generateMapping(String datatype) {
		Mapping mapping = new Mapping();
		Database database = new Database(dbURI);
		database.setJDBCDSN(db.getJdbcURL());
		database.setJDBCDriver("org.hsqldb.jdbcDriver");
		database.setUsername(db.getUser());
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
